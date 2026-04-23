/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring.latency.aggregators

import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLatencyDataApi::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // Required for `kotlin.time.Duration`
class PercentileLatencyAggregatorTest {
    // The test dispatcher uses an internal fake clock. Calls to `delay` in the test code or the
    // system under test will not actually cause the test to wait; instead they will react to the
    // passage of "virtual" time in the test dispatcher, and each test executes with no real delays
    // at
    // all.
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun percentileLatencyAggregator_reportsCorrectValuesAtExpectedInterval() =
        testScope.runTest {
            // Set up an aggregator to report 50th and 75th percentile values, in 1-second reporting
            // windows, of the latencies fed to it. Save all of its reports in lists, the contents
            // of
            // which we'll check at the end of the test.

            val medianLatencies = mutableListOf<Long>()
            val thirdQuartileLatencies = mutableListOf<Long>()
            val sampleCounts = mutableListOf<Int>()

            val aggregator =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f, 75f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    // Every time the aggregator reports new stats, record them in the lists.
                    medianLatencies.add(latencyPercentileNanos[0])
                    thirdQuartileLatencies.add(latencyPercentileNanos[1])
                    sampleCounts.add(sampleCount)
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                // 0.5s delay ensures that each batch falls right in the middle of a 1s aggregation
                // window.
                delay(0.5.seconds)
                aggregator.aggregate(20, 30) // Latency: 10
                aggregator.aggregate(25, 45) // Latency: 20
                aggregator.aggregate(40, 45) // Latency: 5
                aggregator.aggregate(42, 50) // Latency: 8
                aggregator.aggregate(35, 70) // Latency: 35
                // Latency: median = 10, 75th = 20

                delay(1.seconds)
                aggregator.aggregate(101, 110) // Latency: 9
                aggregator.aggregate(119, 125) // Latency: 6
                aggregator.aggregate(107, 125) // Latency: 18
                aggregator.aggregate(115, 130) // Latency: 15
                aggregator.aggregate(133, 150) // Latency: 17
                aggregator.aggregate(127, 152) // Latency: 25
                aggregator.aggregate(135, 156) // Latency: 21
                aggregator.aggregate(148, 190) // Latency: 42
                aggregator.aggregate(146, 207) // Latency: 61
                // Latency: median = 18, 75th = 25

                delay(1.seconds)
                aggregator.aggregate(241, 250) // Latency: 9
                aggregator.aggregate(247, 271) // Latency: 24
                aggregator.aggregate(259, 271) // Latency: 12
                // Latency: median = 12, 75th = 18
            }

            // Let all the inputs and reports play out.
            testScope.advanceTimeBy(10.seconds)

            assertThat(medianLatencies).containsExactly(10L, 18L, 12L).inOrder()
            assertThat(thirdQuartileLatencies).containsExactly(20L, 25L, 18L).inOrder()
            assertThat(sampleCounts).containsExactly(5, 9, 3).inOrder()
        }

    @Test
    fun histogramLatencyAggregator_reportSynchronouslyReportsSynchronously() =
        testScope.runTest {
            // Set up an aggregator to report medians, in 10-second reporting windows, of the
            // latencies
            // fed to it. In the middle of one window, trigger a synchronous report. Save all of the
            // reports in lists, the contents of which we'll check at the end of the test.

            val sampleCounts = mutableListOf<Int>()
            val medianLatencies = mutableListOf<Long>()

            val aggregator =
                PercentileLatencyAggregator.create(
                    window = 10.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    // Every time the aggregator reports new stats, record them in the lists.
                    sampleCounts.add(sampleCount)
                    medianLatencies.add(latencyPercentileNanos[0])
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                // Offset partially into the first reporting window to avoid precision and timing
                // issues.
                delay(1.seconds)

                // First window: 2 calls to `aggregate`; median=35.
                aggregator.aggregate(0, 30)
                aggregator.aggregate(0, 40)
                delay(10.seconds)

                // Second window: 5 calls to `aggregate`, spread out over time.
                aggregator.aggregate(0, 50)
                aggregator.aggregate(0, 60)
                delay(4.seconds) // Now t=15, still in the middle of the second window.
                aggregator.aggregate(0, 70)
                aggregator.aggregate(0, 80)
                aggregator.aggregate(0, 90)
            }

            // Advance to t=12, partway through the second reporting window.
            testScope.advanceTimeBy(12.seconds)

            // Since only the first reporting window has completed, we have only one result.
            assertThat(sampleCounts).containsExactly(2)
            assertThat(medianLatencies).containsExactly(35L)

            aggregator.reportSynchronously()

            // The manually-triggered report includes just the first two inputs in the second
            // window.
            assertThat(sampleCounts).containsExactly(2, 2).inOrder()
            assertThat(medianLatencies).containsExactly(35L, 55L).inOrder()

            // Advance past the end of the second window.
            testScope.advanceTimeBy(10.seconds)

            // The last three inputs get included in the report for the second window. The first two
            // were
            // already accounted for so they don't get double-counted.
            assertThat(sampleCounts).containsExactly(2, 2, 3).inOrder()
            assertThat(medianLatencies).containsExactly(35L, 55L, 80L).inOrder()
        }

    @Test
    fun percentileLatencyAggregator_reportWindowsWithNoInputsProduceNoReport() =
        testScope.runTest {
            // Set up an aggregator to report median values, in 1-second reporting windows, of the
            // latencies fed to it. We'll put a big time gap in the inputs. There should only be
            // reports
            // for the non-empty windows; empty windows should not produce reports.

            var numReports = 0
            val medianLatencies = mutableListOf<Long>()
            val sampleCounts = mutableListOf<Int>()

            val aggregator =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    // Every time the aggregator reports new stats, record them in the lists.
                    numReports += 1
                    medianLatencies.add(latencyPercentileNanos[0])
                    sampleCounts.add(sampleCount)
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                // 0.5s delay ensures that each batch falls right in the middle of a 1s aggregation
                // window.
                delay(0.5.seconds)
                aggregator.aggregate(25, 60) // Latency: 35
                // First reporting window: median latency = 35

                delay(3.seconds)
                // Second and third reporting windows have no input.

                aggregator.aggregate(101, 110) // Latency: 9
                // Fourth reporting window: median latency = 9
            }

            // Let all the inputs and reports play out.
            testScope.advanceTimeBy(10.seconds)

            // The reporting callback should only have been called twice, with the stats for the two
            // non-empty reporting windows.
            assertThat(numReports).isEqualTo(2)
            assertThat(medianLatencies).containsExactly(35L, 9L).inOrder()
            assertThat(sampleCounts).containsExactly(1, 1)
        }

    @Test
    fun percentileLatencyAggregator_allocatesMoreSamplesIfMaxSamplesPerSecondIsTooLow() =
        testScope.runTest {
            // This test verifies something beyond just the correct behavior of the external API.
            // The
            // contract of the aggregator type is that it performs as few allocations as possible
            // after
            // initialization.
            //
            // In particular, if there are more calls to `aggregate` per reporting window than
            // expected,
            // the aggregator has to allocate more latency intervals internally to avoid dropping
            // samples.
            //
            // As above, we store the reported values in lists so we can verify they're correct at
            // the
            // end, even when there are more inputs per window than the aggregator was set up to
            // handle.

            val medianLatencies = mutableListOf<Long>()
            val sampleCounts = mutableListOf<Int>()

            // For a 1-second aggregation window and 30 samples/sec expected, the aggregator
            // preallocates
            // 1.5*30 = 45 samples.
            val aggregator =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    // Every time the aggregator reports new stats, record them in the lists.
                    medianLatencies.add(latencyPercentileNanos[0])
                    sampleCounts.add(sampleCount)
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                // 0.5s delay ensures that each batch falls right in the middle of a 1s aggregation
                // window.
                delay(0.5.seconds)
                // Call `aggregate` more than 45 times in one second. It will need to allocate more
                // samples.
                for (i in 1..49) {
                    aggregator.aggregate(i.toLong(), (10 + 2 * i).toLong()) // Latency: 10+i
                }
                // Median latency = 35 (the median value of i is 25, so the median of 10+i is 35)

                delay(1.seconds)
                // Do it again. This time, the newly-allocated samples should've been recycled into
                // the
                // sample pool, making the pool big enough to accommodate all these calls. So we
                // should not
                // allocate any more samples.
                for (i in 1..49) {
                    aggregator.aggregate(i.toLong(), (15 + 2 * i).toLong()) // Latency: 15+i
                }
                // Median latency = 40 (the median value of i is 25, so the median of 15+i is 40)
            }

            // For the first batch of calls, we expect some new allocations.
            testScope.advanceTimeBy(1.2.seconds)
            val numFirstBatchAllocations = aggregator.numLateIntervalAllocations()
            assertThat(numFirstBatchAllocations).isEqualTo(4)

            // After the second batch, we expect no more new allocations.
            testScope.advanceTimeBy(10.seconds)
            assertThat(aggregator.numLateIntervalAllocations()).isEqualTo(numFirstBatchAllocations)

            // And the reported stats should be correct, showing that the aggregator didn't drop
            // samples.
            assertThat(medianLatencies).containsExactly(35L, 40L).inOrder()
            assertThat(sampleCounts).containsExactly(49, 49)
        }

    @Test
    fun percentileLatencyAggregator_allocatesMorePercentileListsIfReportLambdasTakeALongTime() =
        testScope.runTest {
            // This test verifies something beyond just the correct behavior of the external API.
            // The
            // contract of the aggregator type is that it performs as few allocations as possible
            // after
            // initialization.
            //
            // In particular, the aggregator preallocates a small set of fixed-length lists to pass
            // to the
            // client in each report lambda. If the report lambdas take too long to return, we may
            // not be
            // able to recycle each list in time for a new report to use it. If the aggregator runs
            // out of
            // preallocated lists, it allocates new ones as needed.
            //
            // As above, we store the reported values in lists so we can verify they're correct at
            // the
            // end, even when there are more inputs per window than the aggregator was set up to
            // handle.

            val medianLatencies = mutableListOf<Long>()
            val sampleCounts = mutableListOf<Int>()

            val aggregator =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    // Every time the aggregator reports new stats, record them in the lists.
                    medianLatencies.add(latencyPercentileNanos[0])
                    sampleCounts.add(sampleCount)
                    // Make the report callback take a really long time to finish. This will prevent
                    // recycling
                    // of the preallocated percentile lists.
                    delay(10.seconds)
                }

            val numBatches = 10
            val sampleCountPerBatch = 9

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                // 0.5s delay ensures that each batch falls right in the middle of a 1s aggregation
                // window.
                delay(0.5.seconds)

                repeat(numBatches) {
                    for (i in 1..sampleCountPerBatch) {
                        aggregator.aggregate(i.toLong(), (10 + 2 * i).toLong()) // Latency: 10+i
                    }
                    // Median latency = 15 (since the median of i=1..9 is 5).
                    delay(1.seconds)
                }
            }

            testScope.advanceTimeBy(30.seconds)

            // Since we couldn't recycle percentile lists in time, we expect that new ones were
            // allocated.
            assertThat(aggregator.numLatePercentileListAllocations()).isGreaterThan(0)

            // No extra intervals (input samples) should have been allocated.
            assertThat(aggregator.numLateIntervalAllocations()).isEqualTo(0)

            // The reported stats should be correct, showing that the aggregator didn't drop
            // samples.
            assertThat(medianLatencies).containsExactlyElementsIn(List(numBatches) { 15L })
            assertThat(sampleCounts)
                .containsExactlyElementsIn(List(numBatches) { sampleCountPerBatch })
        }

    @Test
    fun percentileLatencyAggregator_doesNotOrdinarilyNeedToAllocateMorePercentileLists() =
        testScope.runTest {
            val medianLatencies = mutableListOf<Long>()
            val sampleCounts = mutableListOf<Int>()

            val aggregator =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    // Every time the aggregator reports new stats, record them in the lists.
                    medianLatencies.add(latencyPercentileNanos[0])
                    sampleCounts.add(sampleCount)
                    // In contrast to the above test, the report callback finishes pretty quickly.
                    // This allows
                    // percentile list recycling to keep up with demand.
                    delay(1.5.seconds)
                }

            val numBatches = 10
            val sampleCountPerBatch = 9

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                // 0.5s delay ensures that each batch falls right in the middle of a 1s aggregation
                // window.
                delay(0.5.seconds)

                repeat(numBatches) {
                    for (i in 1..sampleCountPerBatch) {
                        aggregator.aggregate(i.toLong(), (10 + 2 * i).toLong())
                    }
                    // Median latency = 15 (since the latency is 10+i)
                    delay(1.seconds)
                }
            }

            testScope.advanceTimeBy(30.seconds)

            assertThat(medianLatencies).hasSize(numBatches)
            assertThat(aggregator.numLateIntervalAllocations()).isEqualTo(0)
            assertThat(aggregator.numLatePercentileListAllocations()).isEqualTo(0)
        }

    @Test
    fun percentileLatencyAggregator_canStartTwoAggregatorsInSameCoroutine() =
        testScope.runTest {
            // Start two aggregators and collect their reports in separate lists. We'll feed
            // different
            // data to each of them. At the end we should see the expected reports on the different
            // inputs
            // they were given.

            val medianLatencies1 = mutableListOf<Long>()
            val sampleCounts1 = mutableListOf<Int>()
            val medianLatencies2 = mutableListOf<Long>()
            val sampleCounts2 = mutableListOf<Int>()

            val aggregator1 =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    medianLatencies1.add(latencyPercentileNanos[0])
                    sampleCounts1.add(sampleCount)
                }
            val aggregator2 =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    medianLatencies2.add(latencyPercentileNanos[0])
                    sampleCounts2.add(sampleCount)
                }

            val numBatches = 4
            val sampleCountPerBatch = 9

            // Feed input into the aggregators in a separate coroutine, so we can specify timing.
            launch {
                // 0.5s delay ensures that each batch falls right in the middle of a 1s aggregation
                // window.
                delay(0.5.seconds)

                // Send input in identical batches, each 1s apart. Each batch should result in a
                // separate
                // report.
                repeat(numBatches) {
                    for (i in 1..sampleCountPerBatch) {
                        aggregator1.aggregate(i.toLong(), (10 + 2 * i).toLong())
                        aggregator2.aggregate((10 + i).toLong(), (40 + 2 * i).toLong())
                    }
                    // aggregator1: median latency = 15 (since the latency is 10+i)
                    // aggregator2: median latency = 35 (since the latency is 30+i)
                    delay(1.seconds)
                }
            }

            testScope.advanceTimeBy(30.seconds)

            // We see the results of reports for all four batches for each aggregator.
            assertThat(medianLatencies1).containsExactlyElementsIn(List(numBatches) { 15L })
            assertThat(sampleCounts1)
                .containsExactlyElementsIn(List(numBatches) { sampleCountPerBatch })
            assertThat(medianLatencies2).containsExactlyElementsIn(List(numBatches) { 35L })
            assertThat(sampleCounts2)
                .containsExactlyElementsIn(List(numBatches) { sampleCountPerBatch })
        }

    @Test
    fun percentileLatencyAggregator_canStopOneAggregatorWhileTheOtherContinues() =
        testScope.runTest {
            // Start two aggregators and collect their reports in separate lists. We'll feed data to
            // both
            // of them, as in the test above, but stop one of them halfway through. That one should
            // not
            // report any results after being stopped.

            val medianLatencies1 = mutableListOf<Long>()
            val sampleCounts1 = mutableListOf<Int>()
            val medianLatencies2 = mutableListOf<Long>()
            val sampleCounts2 = mutableListOf<Int>()

            val aggregator1 =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    medianLatencies1.add(latencyPercentileNanos[0])
                    sampleCounts1.add(sampleCount)
                }
            val aggregator2 =
                PercentileLatencyAggregator.create(
                    window = 1.seconds,
                    percentiles = listOf(50f),
                    expectedSamplesPerSecond = 30,
                    testScope.backgroundScope,
                ) { latencyPercentileNanos: List<Long>, sampleCount: Int ->
                    medianLatencies2.add(latencyPercentileNanos[0])
                    sampleCounts2.add(sampleCount)
                }

            val numBatches = 4
            val sampleCountPerBatch = 9

            // Feed input into the aggregators in a separate coroutine, so we can specify timing.
            // This
            // block is identical to that in the previous test
            // (canStartTwoAggregatorsInSameCoroutine).
            launch {
                // 0.5s delay ensures that each batch falls right in the middle of a 1s aggregation
                // window.
                delay(0.5.seconds)

                // Send input in identical batches, each 1s apart. Each batch should result in a
                // separate
                // report.
                repeat(numBatches) {
                    for (i in 1..sampleCountPerBatch) {
                        aggregator1.aggregate(i.toLong(), (10 + 2 * i).toLong())
                        aggregator2.aggregate((10 + i).toLong(), (40 + 2 * i).toLong())
                    }
                    // aggregator1: median latency = 15 (since the latency is 10+i)
                    // aggregator2: median latency = 35 (since the latency is 30+i)
                    delay(1.seconds)
                }
            }

            // Cancel aggregator1's job just before the third batch is to be reported, but after it
            // has
            // received inputs for that batch.
            testScope.advanceTimeBy(2.75.seconds)
            aggregator1.job().cancelAndJoin()

            // Keep running for a while to let everything play out.
            testScope.advanceTimeBy(8.seconds)

            // aggregator1 only reported on inputs from the first two batches.
            assertThat(medianLatencies1).containsExactlyElementsIn(List(2) { 15L })
            assertThat(sampleCounts1).containsExactlyElementsIn(List(2) { sampleCountPerBatch })

            // aggregator2 reported on all the batches.
            assertThat(medianLatencies2).containsExactlyElementsIn(List(numBatches) { 35L })
            assertThat(sampleCounts2)
                .containsExactlyElementsIn(List(numBatches) { sampleCountPerBatch })
        }

    @Test
    fun percentileLatencyAggregator_factoryThrowsOnNonPositiveWindowDuration() {
        assertFailsWith(IllegalStateException::class) {
            PercentileLatencyAggregator.create(
                window = 0.seconds,
                percentiles = listOf(50f),
                expectedSamplesPerSecond = 30,
                testScope.backgroundScope,
            ) { _: List<Long>, _: Int ->
            }
        }
    }

    @Test
    fun percentileLatencyAggregator_factoryThrowsOnEmptyPercentileList() {
        assertFailsWith(IllegalStateException::class) {
            PercentileLatencyAggregator.create(
                window = 1.seconds,
                percentiles = listOf<Float>(),
                expectedSamplesPerSecond = 30,
                testScope.backgroundScope,
            ) { _: List<Long>, _: Int ->
            }
        }
    }

    @Test
    fun percentileLatencyAggregator_factoryThrowsOnNegativePercentileList() {
        assertFailsWith(IllegalStateException::class) {
            PercentileLatencyAggregator.create(
                window = 1.seconds,
                percentiles = listOf<Float>(-5f),
                expectedSamplesPerSecond = 30,
                testScope.backgroundScope,
            ) { _: List<Long>, _: Int ->
            }
        }
    }

    @Test
    fun percentileLatencyAggregator_factoryThrowsOnPercentileListAbove100() {
        assertFailsWith(IllegalStateException::class) {
            PercentileLatencyAggregator.create(
                window = 1.seconds,
                percentiles = listOf<Float>(105f),
                expectedSamplesPerSecond = 30,
                testScope.backgroundScope,
            ) { _: List<Long>, _: Int ->
            }
        }
    }

    @Test
    fun percentileLatencyAggregator_factoryThrowsOnNonPositiveSamplesPerSecond() {
        assertFailsWith(IllegalStateException::class) {
            PercentileLatencyAggregator.create(
                window = 1.seconds,
                percentiles = listOf(50f),
                expectedSamplesPerSecond = 0,
                testScope.backgroundScope,
            ) { _: List<Long>, _: Int ->
            }
        }
    }

    @Test
    fun percentileLatencyAggregator_percentilesAreExactlyInputsIfPercentileMatchesIndexExactly() =
        testScope.runTest {
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies = listOf(10, 23, 31, 33, 46),
                        percentiles = listOf(0f, 25f, 50f, 75f, 100f),
                    )
                )
                .isEqualTo(listOf<Long>(10, 23, 31, 33, 46))
        }

    @Test
    fun percentileLatencyAggregator_percentilesAreExactlyInputsIfFinitePrecisionPercentileMatchesIndexExactly() =
        testScope.runTest {
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies = listOf(10, 23, 31, 33, 46, 61, 68, 75),
                        // These percentiles (divided by 100) don't have an exact finite binary
                        // representation,
                        // but they correspond exactly to indices of the list of input latencies.
                        percentiles = listOf(2f / 7f * 100f, 5f / 7f * 100f),
                    )
                )
                .isEqualTo(listOf<Long>(31, 61))
        }

    @Test
    fun percentileLatencyAggregator_percentilesAreExactlyInputsIfPercentileMatchesExtremelyClosely() =
        testScope.runTest {
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies = listOf(10, 23, 31, 33, 46),
                        percentiles = listOf(74.999995f, 75.000005f),
                    )
                )
                .isEqualTo(listOf<Long>(33, 33))
        }

    @Test
    fun percentileLatencyAggregator_medianOfOddLengthListIsMiddle() =
        testScope.runTest {
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies = listOf(10, 23, 31, 33, 46, 61, 68),
                        percentiles = listOf(50f),
                    )
                )
                .isEqualTo(listOf<Long>(33))
        }

    @Test
    fun percentileLatencyAggregator_medianOfEvenLengthListIsAverageOfMiddleTwo() =
        testScope.runTest {
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies = listOf(10, 23, 31, 33, 46, 61),
                        percentiles = listOf(50f),
                    )
                )
                .isEqualTo(listOf<Long>(32))
        }

    @Test
    fun percentileLatencyAggregator_medianOfEvenLengthListRoundsUpAverage() =
        testScope.runTest {
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies = listOf(10, 24, 31, 33),
                        percentiles = listOf(50f),
                    )
                )
                // Actual median is 27.5. It should round up.
                .isEqualTo(listOf<Long>(28))
        }

    @Test
    fun percentileLatencyAggregator_percentilesBetweenIndicesAreWeightedAveragesRoundedToInts() =
        testScope.runTest {
            val latencies = listOf<Long>(70, 80, 90)

            // The 50th and 100th percentiles are items from the list.
            assertThat(
                    useAggregatorToComputePercentiles(latencies, percentiles = listOf(50f, 100f))
                )
                .isEqualTo(listOf<Long>(80, 90))

            // In the interval from the 50th to the 100th percentile (80 to 90), the 60th, 70th,
            // 80th, and
            // 90th are spaced evenly at 1/5 of the overall interval (1/5 of 10 = 2).
            assertThat(useAggregatorToComputePercentiles(latencies, percentiles = listOf(70f, 80f)))
                .isEqualTo(listOf<Long>(84, 86))

            // In the interval from the 70th to the 80th percentile (84 to 86), the 71st, 72nd,...
            // 79th
            // are spaced evenly at 1/10 of the overall interval (1/10 of 2 = 0.2).
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies,
                        percentiles = listOf(72f, 73f, 75f),
                    )
                )
                .isEqualTo(listOf<Long>(84, 85, 85))

            // In the interval from the 72nd to the 73rd percentile (84.4 to 84.6), the 72.1th,
            // 72.2th,...
            // 72.9th are spaced evenly at 1/10 of the overall interval (1/10 of 0.2 = 0.02).
            val actualPercentiles =
                useAggregatorToComputePercentiles(
                    latencies,
                    percentiles = listOf(72.4f, 72.5f, 72.6f),
                )
            assertThat(actualPercentiles[0]).isEqualTo(84) // 84.48 rounds down.
            // With infinite precision, we can tell that the 72.5-th percentile should be exactly
            // 84.5.
            // But 0.725 doesn't have a finite binary expansion, so rounding may go either way.
            assertThat(actualPercentiles[1]).isAnyOf(84L, 85L)
            assertThat(actualPercentiles[2]).isEqualTo(85) // 84.52 rounds up.
        }

    @Test
    fun percentileLatencyAggregator_allPercentilesAreSameForSingletonList() =
        testScope.runTest {
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies = listOf(23),
                        percentiles = listOf(0f, 10f, 5f / 7f * 100f, 100f),
                    )
                )
                .isEqualTo(listOf<Long>(23, 23, 23, 23))
        }

    @Test
    fun percentileLatencyAggregator_percentilesAreWeightedAveragesForTwoElementList() =
        testScope.runTest {
            assertThat(
                    useAggregatorToComputePercentiles(
                        latencies = listOf(23, 33),
                        percentiles = listOf(0f, 30f, 58f, 100f),
                    )
                )
                // The 58th percentile is 28.8, which rounds to 29.
                .isEqualTo(listOf<Long>(23, 26, 29, 33))
        }

    private suspend fun useAggregatorToComputePercentiles(
        latencies: List<Long>,
        percentiles: List<Float>,
    ): List<Long> {
        val actualPercentiles = mutableListOf<Long>()

        val aggregator =
            PercentileLatencyAggregator.create(
                window = 1.seconds,
                percentiles = percentiles,
                expectedSamplesPerSecond = 30,
                testScope.backgroundScope,
            ) { latencyPercentileNanos: List<Long>, _: Int ->
                actualPercentiles.addAll(latencyPercentileNanos)
            }

        for (latency in latencies) {
            aggregator.aggregate(0, latency)
        }

        delay(2.seconds)
        aggregator.job().cancelAndJoin()

        return actualPercentiles
    }
}
