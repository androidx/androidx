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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalLatencyDataApi::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // Required for `kotlin.time.Duration`
class HistogramLatencyAggregatorTest {
    // The test dispatcher uses an internal fake clock. Calls to `delay` in the test code or the
    // system under test will not actually cause the test to wait; instead they will react to the
    // passage of "virtual" time in the test dispatcher, and each test executes with no real delays
    // at
    // all.
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun histogramLatencyAggregator_reportsOneMoreBucketThanTheNumberOfBoundaries() =
        testScope.runTest {
            var numBuckets = -1
            val unused =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(10L, 20L, 30L, 50L), // Four boundaries.
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    numBuckets = bucketCounts.size
                }

            // Let it run for a while so that the report callback is called once.
            testScope.advanceTimeBy(15.seconds)

            // There were four bucket boundaries, so they define five buckets.
            assertThat(numBuckets).isEqualTo(5)
        }

    @Test
    fun histogramLatencyAggregator_reportsTwoBucketsWhenSetUpWithASingleBoundary() =
        testScope.runTest {
            var numBuckets = -1
            val unused =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(20L), // One boundary.
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    numBuckets = bucketCounts.size
                }

            // Let it run for a while so that the report callback is called once.
            testScope.advanceTimeBy(15.seconds)

            // There was just one bucket boundary, which defines two buckets.
            assertThat(numBuckets).isEqualTo(2)
        }

    @Test
    fun histogramLatencyAggregator_reportsOneBucketWhenSetUpWithNoBoundaries() =
        testScope.runTest {
            var numBuckets = -1
            val unused =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(), // No boundaries.
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    numBuckets = bucketCounts.size
                }

            // Let it run for a while so that the report callback is called once.
            testScope.advanceTimeBy(15.seconds)

            // There were no bucket boundaries, so it defines just a single bucket.
            assertThat(numBuckets).isEqualTo(1)
        }

    @Test
    fun histogramLatencyAggregator_lowerBoundsAreInclusive() =
        testScope.runTest {
            // Set up a histogram aggregator with buckets (-inf, 10), [10, 20), [20, +inf).
            val countsUnder10 = mutableListOf<Int>()
            val countsUnder20 = mutableListOf<Int>()
            val counts20OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(10L, 20L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder10.add(bucketCounts[0])
                    countsUnder20.add(bucketCounts[1])
                    counts20OrOver.add(bucketCounts[2])
                }

            aggregator.aggregate(0, 5)
            aggregator.aggregate(0, 10) // Goes in the middle bucket.
            aggregator.aggregate(0, 15)
            aggregator.aggregate(0, 20) // Goes in the last bucket.

            // Let it run for a while so that the report callback is called once.
            testScope.advanceTimeBy(15.seconds)

            assertThat(countsUnder10).containsExactly(1)
            assertThat(countsUnder20).containsExactly(2)
            assertThat(counts20OrOver).containsExactly(1)
        }

    @Test
    fun histogramLatencyAggregator_countsCorrectlyAndReportsAtExpectedInterval() =
        testScope.runTest {
            // Set up a histogram aggregator with buckets (-inf, 10), [10, 20), [20, +inf).
            val countsUnder10 = mutableListOf<Int>()
            val countsUnder20 = mutableListOf<Int>()
            val counts20OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(10L, 20L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder10.add(bucketCounts[0])
                    countsUnder20.add(bucketCounts[1])
                    counts20OrOver.add(bucketCounts[2])
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                delay(1.seconds)

                // First window: 2 vals in (-inf, 10), 2 in [10, 20), 2 in [20, +inf).
                aggregator.aggregate(0, 5)
                aggregator.aggregate(0, 20)
                aggregator.aggregate(0, -20)
                aggregator.aggregate(0, 25)
                aggregator.aggregate(0, 10)
                aggregator.aggregate(0, 15)
                delay(10.seconds)

                // Second window: 2 vals in (-inf, 10), 1 in [10, 20), 0 in [20, +inf).
                aggregator.aggregate(0, 14)
                aggregator.aggregate(0, 8)
                aggregator.aggregate(0, 9)
                delay(10.seconds)

                // Third window: 1 val in (-inf, 10), 3 in [10, 20), 2 in [20, +inf).
                // For coverage purposes, the intervals here start at a nonzero startNanos.
                aggregator.aggregate(500, 522)
                aggregator.aggregate(500, 508)
                aggregator.aggregate(500, 516)
                aggregator.aggregate(500, 519)
                aggregator.aggregate(500, 514)
                aggregator.aggregate(500, 529)
            }

            // To report three windows, advance past the report time of the third batch (t=30s).
            testScope.advanceTimeBy(35.seconds)

            assertThat(countsUnder10).containsExactly(2, 2, 1).inOrder()
            assertThat(countsUnder20).containsExactly(2, 1, 3).inOrder()
            assertThat(counts20OrOver).containsExactly(2, 0, 2).inOrder()
        }

    @Test
    fun histogramLatencyAggregator_countsCorrectlyInTwoBuckets() =
        testScope.runTest {
            // Set up a histogram aggregator with one boundary, which produces two half-infinite
            // buckets.
            val countsUnder20 = mutableListOf<Int>()
            val counts20OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(20L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder20.add(bucketCounts[0])
                    counts20OrOver.add(bucketCounts[1])
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                delay(1.seconds)

                // First window: 2 vals in (-inf, 20), 1 in [20, +inf).
                aggregator.aggregate(0, 5)
                aggregator.aggregate(0, 25)
                aggregator.aggregate(0, -20)
                delay(10.seconds)

                // Second window: 1 val in (-inf, 20), 2 in [20, +inf).
                aggregator.aggregate(0, 19)
                aggregator.aggregate(0, 30)
                aggregator.aggregate(0, 20)
            }

            // To report two windows, advance past the report time of the second one (t=20s).
            testScope.advanceTimeBy(25.seconds)

            assertThat(countsUnder20).containsExactly(2, 1).inOrder()
            assertThat(counts20OrOver).containsExactly(1, 2).inOrder()
        }

    @Test
    fun histogramLatencyAggregator_countsCorrectlyInOneBucket() =
        testScope.runTest {
            // Set up a histogram aggregator with no boundaries, which reduces to just counting the
            // number
            // of `aggregate` calls.
            val counts = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    counts.add(bucketCounts[0])
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                delay(1.seconds)

                // First window: 2 calls to `aggregate`.
                aggregator.aggregate(0, 65)
                aggregator.aggregate(0, -200)
                delay(10.seconds)

                // Second window: 3 calls to `aggregate`.
                aggregator.aggregate(0, 14)
                aggregator.aggregate(0, 8)
                aggregator.aggregate(0, 9)
            }

            // To report two windows, advance past the report time of the second one (t=20s).
            testScope.advanceTimeBy(25.seconds)

            assertThat(counts).containsExactly(2, 3).inOrder()
        }

    @Test
    fun histogramLatencyAggregator_reportsAllZeroWhenNoInput() =
        testScope.runTest {
            // Set up a histogram aggregator with buckets (-inf, 50), [50, +inf).
            val countsUnder50 = mutableListOf<Int>()
            val counts50OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(50L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder50.add(bucketCounts[0])
                    counts50OrOver.add(bucketCounts[1])
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                // Two windows pass by with no initial input.
                delay(21.seconds)

                // In the third window, there's input. 2 vals below 50, 1 val above.
                aggregator.aggregate(0, 10)
                aggregator.aggregate(0, 20)
                aggregator.aggregate(0, 60)
                delay(10.seconds)

                // Two more windows pass with no input.
                delay(20.seconds)

                // In the sixth window, there's input. 1 val below 50, 2 vals above.
                aggregator.aggregate(0, 30)
                aggregator.aggregate(0, 80)
                aggregator.aggregate(0, 90)

                // Below we'll let enough time pass that a seventh window passes with no input.
            }

            // In 75 seconds, 7 windows should be reported.
            testScope.advanceTimeBy(75.seconds)

            assertThat(countsUnder50).containsExactly(0, 0, 2, 0, 0, 1, 0).inOrder()
            assertThat(counts50OrOver).containsExactly(0, 0, 1, 0, 0, 2, 0).inOrder()
        }

    @Test
    fun histogramLatencyAggregator_reportSynchronouslyReportsSynchronously() =
        testScope.runTest {
            // Set up a histogram aggregator with no boundaries, which reduces to just counting the
            // number
            // of `aggregate` calls.
            val counts = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    counts.add(bucketCounts[0])
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                delay(1.seconds)

                // First window: 2 calls to `aggregate`.
                aggregator.aggregate(0, 99)
                aggregator.aggregate(0, 99)
                delay(10.seconds)

                // Second window: 3 calls to `aggregate`, spread out over time.
                aggregator.aggregate(0, 99) // A. time=11
                delay(4.seconds)
                aggregator.aggregate(0, 99) // B. time=15
                delay(4.seconds)
                aggregator.aggregate(0, 99) // C. time=19
            }

            // Advance to time=12, partway into the second reporting window.
            testScope.advanceTimeBy(12.seconds)

            // Since only the first reporting window has completed, we have only one result.
            assertThat(counts).containsExactly(2)

            aggregator.reportSynchronously()

            // The manually-triggered report includes just call A.
            assertThat(counts).containsExactly(2, 1).inOrder()

            // Advance past the end of the second window.
            testScope.advanceTimeBy(10.seconds)

            // Calls B and C get included in the second window. Call A was already accounted for so
            // it
            // doesn't get double-counted.
            assertThat(counts).containsExactly(2, 1, 2).inOrder()
        }

    @Test
    fun histogramLatencyAggregator_canStartTwoAggregatorsInSameCoroutine() =
        testScope.runTest {
            // Set up two histogram aggregators with buckets (-inf, 50), [50, +inf).
            val countsUnder50 = mutableListOf<Int>()
            val counts50OrOver = mutableListOf<Int>()
            val othercountsUnder50 = mutableListOf<Int>()
            val othercounts50OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(50L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder50.add(bucketCounts[0])
                    counts50OrOver.add(bucketCounts[1])
                }
            val otherAggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(50L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    othercountsUnder50.add(bucketCounts[0])
                    othercounts50OrOver.add(bucketCounts[1])
                }

            // Feed input into the aggregators in a separate coroutine, so we can specify timing.
            launch {
                delay(1.seconds)

                // First window.
                aggregator.aggregate(0, 10)
                otherAggregator.aggregate(0, 60)
                otherAggregator.aggregate(0, 70)
                delay(10.seconds)

                // Second window.
                otherAggregator.aggregate(0, 20)
                aggregator.aggregate(0, 60)
                aggregator.aggregate(0, 70)
                aggregator.aggregate(0, 80)
                delay(10.seconds)
            }

            // In 25 seconds, 2 windows should be reported.
            testScope.advanceTimeBy(25.seconds)

            assertThat(countsUnder50).containsExactly(1, 0).inOrder()
            assertThat(counts50OrOver).containsExactly(0, 3).inOrder()
            assertThat(othercountsUnder50).containsExactly(0, 1).inOrder()
            assertThat(othercounts50OrOver).containsExactly(2, 0).inOrder()
        }

    @Test
    fun histogramLatencyAggregator_canStopOneAggregatorWhileTheOtherContinues() =
        testScope.runTest {
            // Set up two histogram aggregators with buckets (-inf, 50), [50, +inf).
            val countsUnder50 = mutableListOf<Int>()
            val counts50OrOver = mutableListOf<Int>()
            val othercountsUnder50 = mutableListOf<Int>()
            val othercounts50OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(50L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder50.add(bucketCounts[0])
                    counts50OrOver.add(bucketCounts[1])
                }
            val otherAggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(50L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    othercountsUnder50.add(bucketCounts[0])
                    othercounts50OrOver.add(bucketCounts[1])
                }

            // Feed input into the aggregators in a separate coroutine, so we can specify timing.
            // The
            // inputs in this test case are identical to the one above.
            launch {
                delay(1.seconds)

                // First window.
                aggregator.aggregate(0, 10)
                otherAggregator.aggregate(0, 60)
                otherAggregator.aggregate(0, 70)
                delay(10.seconds)

                // Below, we'll cancel otherAggregator's job before the second window starts.

                // Second window.
                otherAggregator.aggregate(0, 20)
                aggregator.aggregate(0, 60)
                aggregator.aggregate(0, 70)
                aggregator.aggregate(0, 80)
                delay(10.seconds)
            }

            // Cancel otherAggregator's job before the second window ends.
            testScope.advanceTimeBy(15.seconds)
            otherAggregator.job().cancelAndJoin()

            // Let 10 more seconds pass, for a total of 25: two full windows have passed.
            testScope.advanceTimeBy(10.seconds)

            // The first aggregator reported on both windows.
            assertThat(countsUnder50).containsExactly(1, 0).inOrder()
            assertThat(counts50OrOver).containsExactly(0, 3).inOrder()

            // otherAggregator reported on only the first window.
            assertThat(othercountsUnder50).containsExactly(0)
            assertThat(othercounts50OrOver).containsExactly(2)
        }

    @Test
    fun histogramLatencyAggregator_cancelingScopeCancelsAggregator() =
        testScope.runTest {
            // Set up a histogram aggregator with buckets (-inf, 50), [50, +inf).
            val countsUnder50 = mutableListOf<Int>()
            val counts50OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(50L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder50.add(bucketCounts[0])
                    counts50OrOver.add(bucketCounts[1])
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                delay(1.seconds)

                // First window.
                aggregator.aggregate(0, 10)
                aggregator.aggregate(0, 20)
                aggregator.aggregate(0, 60)
                delay(10.seconds)

                // Below, we'll cancel otherAggregator's job before the second window ends.

                // Second window.
                aggregator.aggregate(0, 30)
                aggregator.aggregate(0, 80)
                aggregator.aggregate(0, 90)
            }

            // Cancel the background scope before the second window ends.
            testScope.advanceTimeBy(15.seconds)
            testScope.backgroundScope.cancel()

            // Let 10 more seconds pass, for a total of 25: two full windows have passed.
            testScope.advanceTimeBy(10.seconds)

            // The aggregator reported only on the first window.
            assertThat(countsUnder50).containsExactly(2)
            assertThat(counts50OrOver).containsExactly(1)
        }

    @Test
    fun histogramLatencyAggregator_allocatesMoreHistogramsIfReportLambdasTakeALongTime() =
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
            // Set up a histogram aggregator with buckets (-inf, 50), [50, +inf).
            val countsUnder50 = mutableListOf<Int>()
            val counts50OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(50L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder50.add(bucketCounts[0])
                    counts50OrOver.add(bucketCounts[1])
                    // Make the report callback take a really long time to finish. This will prevent
                    // recycling
                    // of the preallocated histogram lists.
                    delay(70.seconds)
                }

            launch {
                delay(1.seconds)
                repeat(15) {
                    aggregator.aggregate(0, 30)
                    delay(10.seconds)
                }
            }

            // Let enough time pass for all the `aggregate` calls and all the report callbacks to
            // finish.
            // 255 seconds covers 25 full report windows. Only the first 15 had `aggregate` calls in
            // them.
            testScope.advanceTimeBy(255.seconds)

            // Since we couldn't recycle histograms in time, we expect that new ones were allocated.
            assertThat(aggregator.numLateHistogramAllocations()).isGreaterThan(0)

            // The reported histograms should be correct, showing that the aggregator didn't drop
            // data.
            // The first 15 windows had `aggregate` calls, then 10 more empty windows.
            assertThat(countsUnder50)
                .containsExactlyElementsIn(List(15) { 1 } + List(10) { 0 })
                .inOrder()
            assertThat(counts50OrOver).containsExactlyElementsIn(List(25) { 0 })
        }

    @Test
    fun histogramLatencyAggregator_doesNotOrdinarilyNeedToAllocateMoreHistograms() =
        testScope.runTest {
            // Set up a histogram aggregator with buckets (-inf, 50), [50, +inf).
            val countsUnder50 = mutableListOf<Int>()
            val counts50OrOver = mutableListOf<Int>()
            val aggregator =
                HistogramLatencyAggregator.create(
                    window = 10.seconds,
                    inclusiveLowerBoundsNanos = listOf(50L),
                    scope = testScope.backgroundScope,
                ) { bucketCounts: IntArray ->
                    countsUnder50.add(bucketCounts[0])
                    counts50OrOver.add(bucketCounts[1])
                    // In contrast to the above test, the report callback finishes relatively
                    // quickly. This
                    // allows histogram list recycling to keep up with demand.
                    delay(15.seconds)
                }

            launch {
                delay(1.seconds)
                repeat(15) {
                    aggregator.aggregate(0, 30)
                    delay(10.seconds)
                }
            }

            // Let enough time pass for all the `aggregate` calls and all the report callbacks to
            // finish.
            // 255 seconds covers 25 full report windows. Only the first 15 had `aggregate` calls in
            // them.
            testScope.advanceTimeBy(255.seconds)

            // We expect no new histograms were allocated.
            assertThat(aggregator.numLateHistogramAllocations()).isEqualTo(0)

            // The reported histograms should be correct, showing that the aggregator didn't drop
            // data.
            // The first 15 windows had `aggregate` calls, then 10 more empty windows.
            assertThat(countsUnder50)
                .containsExactlyElementsIn(List(15) { 1 } + List(10) { 0 })
                .inOrder()
            assertThat(counts50OrOver).containsExactlyElementsIn(List(25) { 0 })
        }

    @Test
    fun histogramLatencyAggregator_factoryThrowsOnNonPositiveWindowDuration() {
        assertFailsWith(IllegalStateException::class) {
            HistogramLatencyAggregator.create(
                window = 0.seconds,
                inclusiveLowerBoundsNanos = listOf(50L),
                scope = testScope.backgroundScope,
            ) { _: IntArray ->
            }
        }
    }

    @Test
    fun histogramLatencyAggregator_factoryThrowsOnOutOfOrderBucketBoundaries() {
        assertFailsWith(IllegalStateException::class) {
            HistogramLatencyAggregator.create(
                window = 10.seconds,
                inclusiveLowerBoundsNanos = listOf<Long>(10, 30, 20, 40),
                scope = testScope.backgroundScope,
            ) { _: IntArray ->
            }
        }
    }

    @Test
    fun histogramLatencyAggregator_factoryThrowsOnNonStrictlyIncreasingBucketBoundaries() {
        assertFailsWith(IllegalStateException::class) {
            HistogramLatencyAggregator.create(
                window = 10.seconds,
                inclusiveLowerBoundsNanos = listOf<Long>(10, 10, 30, 40),
                scope = testScope.backgroundScope,
            ) { _: IntArray ->
            }
        }
        assertFailsWith(IllegalStateException::class) {
            HistogramLatencyAggregator.create(
                window = 10.seconds,
                inclusiveLowerBoundsNanos = listOf<Long>(10, 30, 30, 40),
                scope = testScope.backgroundScope,
            ) { _: IntArray ->
            }
        }
        assertFailsWith(IllegalStateException::class) {
            HistogramLatencyAggregator.create(
                window = 10.seconds,
                inclusiveLowerBoundsNanos = listOf<Long>(10, 30, 40, 40),
                scope = testScope.backgroundScope,
            ) { _: IntArray ->
            }
        }
    }

    @Test
    fun histogramLatencyAggregator_canRunInRealTimeWithoutDeadlockOrCrashing() {
        // Rather than using a fake clock, this test uses a real one and continually feeds input
        // into
        // the aggregator, to stress-test the synchronization of aggregation and reporting.

        // Set up a histogram aggregator with buckets (-inf, 50), [50, +inf). Don't bother saving
        // the
        // reported histograms; just count reports.
        var numReports = 0
        val aggregator =
            HistogramLatencyAggregator.create(
                window = 0.125.seconds,
                inclusiveLowerBoundsNanos = listOf(50L),
                scope = CoroutineScope(Dispatchers.Default),
            ) { _: IntArray ->
                numReports += 1
            }

        val startTimeMillis = System.currentTimeMillis()
        runBlocking {
            launch {
                // Send input to the aggregator for 2 seconds of real time.
                while (System.currentTimeMillis() - startTimeMillis <= 2_000) {
                    aggregator.aggregate(0, 10)
                }
            }
        }

        assertThat(aggregator.job().isActive).isTrue()
        assertThat(numReports).isGreaterThan(0)
    }
}
