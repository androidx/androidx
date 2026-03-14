/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.tracing.wire

import androidx.tracing.ExperimentalContextPropagation
import java.io.File
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TracingDemoTest {
    internal val random = Random(42)
    internal val forkSize = 2
    internal val multiplier = 1000
    internal val inputSize = forkSize * multiplier

    // Tracks the number of batches completed
    @Volatile internal var count = 0L
    internal val driver =
        TraceDriver(sink = TraceSink(sequenceId = 1, directory = File("/tmp")), isEnabled = true)
    internal val tracer = driver.tracer
    internal val counter = tracer.counter(category = "Counters", name = "Batches Completed")

    @Test
    internal fun testSimpleTracingTest() = runBlocking {
        driver.use {
            withContext(context = Dispatchers.Default) {
                tracer.traceCoroutine(
                    category = "category",
                    name = "trace",
                    metadataBlock = {
                        addMetadataEntry("context", "basic trace with 1 suspension point")
                        addCallStackEntry(
                            name = TracingDemoTest::class.java.name,
                            sourceFile = "/path/to/TracingDemoTest.kt",
                            lineNumber = -1,
                        )
                    },
                ) {
                    tracer.instant(category = "category", name = "Delaying") {
                        addMetadataEntry("key", "value")
                    }
                    coroutineScope { delay(10L) }
                }
            }
        }
    }

    @Test
    internal fun testSimpleTracingTestWithCorrelationIds() = runBlocking {
        driver.use {
            withContext(context = Dispatchers.Default) {
                val correlationId = 1L
                tracer.traceCoroutine(
                    "category",
                    "section",
                    metadataBlock = { addCorrelationId(correlationId) },
                ) {
                    delay(100L)
                }
                tracer.traceCoroutine(
                    "category",
                    "section2",
                    metadataBlock = { addCorrelationId(correlationId) },
                ) {
                    delay(1000L)
                }
            }
        }
    }

    @Test
    internal fun testTracingEndToEnd(): Unit = runBlocking {
        driver.use {
            withContext(context = Dispatchers.Default) {
                tracer.traceCoroutine(
                    category = "category",
                    name = "begin",
                    metadataBlock = { addMetadataEntry("context", "end to end tracing test") },
                ) {
                    coroutineScope {
                        delay(20L)
                        tracer.traceCoroutine(
                            category = "category",
                            name = "histograms-end-to-end",
                        ) {
                            computeHistograms()
                        }
                        tracer.traceCoroutine(category = "category", name = "end") { delay(20L) }
                        delay(40L)
                    }
                }
            }
        }
    }

    @Test
    // The amount of time spent sleeping does not affect the outcome of the test.
    @Suppress("BanThreadSleep")
    @OptIn(ExperimentalContextPropagation::class)
    internal fun testManualContextPropagation() {
        driver.use {
            val token = tracer.tokenForManualPropagation()
            val threads = mutableListOf<Thread>()
            threads += thread {
                tracer.trace(category = "category", name = "first", token = token) {
                    Thread.sleep(100L)
                    tracer.trace(category = "category", name = "second", token = token) {
                        Thread.sleep(200L)

                        runBlocking {
                            tracer.traceCoroutine(
                                category = "category",
                                name = "third",
                                token = token,
                            ) {
                                delay(200L)
                            }
                        }
                    }
                }
            }
            threads.forEach { it.join() }
        }
    }

    internal suspend fun computeHistograms(): Map<Int, Int> {
        val input = List(inputSize) { random.nextInt(0, 100_000) }
        val batches = input.chunked(multiplier)
        return coroutineScope {
            val jobs = mutableListOf<Deferred<Map<Int, Int>>>()
            batches.forEachIndexed { index, batch ->
                jobs += async {
                    tracer.traceCoroutine(category = "category", name = "histograms-batch-$index") {
                        computeHistogram(batch)
                    }
                }
            }
            val histograms = jobs.awaitAll()
            val output =
                tracer.traceCoroutine(category = "category", name = "merge-histograms") {
                    mergeHistograms(input, histograms)
                }
            output
        }
    }

    internal suspend fun computeHistogram(list: List<Int>): Map<Int, Int> {
        val frequency = mutableMapOf<Int, Int>()
        for (element in list) {
            val count = frequency[element] ?: 0
            frequency[element] = count + 1
        }
        delay(random.nextLong(10L, 20L)) // Waterfall
        count += 1
        counter.setValue(count)
        return frequency
    }

    internal suspend fun mergeHistograms(
        input: List<Int>,
        histograms: List<Map<Int, Int>>,
    ): Map<Int, Int> {
        val frequency = mutableMapOf<Int, Int>()
        for (element in input) {
            var count = 0
            for (histogram in histograms) {
                count += histogram[element] ?: 0
            }
            frequency[element] = count
        }
        delay(random.nextLong(10L, 20L)) // Waterfall
        return frequency
    }
}
