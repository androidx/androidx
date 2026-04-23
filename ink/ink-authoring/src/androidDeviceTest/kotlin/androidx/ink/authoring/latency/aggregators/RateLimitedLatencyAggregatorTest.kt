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
import kotlinx.coroutines.cancel
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
class RateLimitedLatencyAggregatorTest {
    // The test dispatcher uses an internal fake clock. Calls to `delay` in the test code or the
    // system under test will not actually cause the test to wait; instead they will react to the
    // passage of "virtual" time in the test dispatcher, and each test executes with no real delays
    // at
    // all.
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun rateLimitedLatencyAggregator_reportsAtExpectedInterval() =
        testScope.runTest {
            val sampleStarts = mutableListOf<Long>()
            val sampleEnds = mutableListOf<Long>()

            val aggregator =
                RateLimitedLatencyAggregator.create(
                    period = 1.seconds,
                    testScope.backgroundScope,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts.add(startNanos)
                    sampleEnds.add(endNanos)
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                delay(0.5.seconds)
                callAggregateWith(aggregator, 10L..13L, 20L..23L)

                delay(1.seconds)
                aggregator.aggregate(20, 35)

                delay(1.seconds)
                callAggregateWith(aggregator, 1L..1_000L, 2L..1_001L)
            }

            testScope.advanceTimeBy(10.seconds)

            // The last input in each second should have been reported.
            assertThat(sampleStarts).containsExactly(13L, 20L, 1_000L).inOrder()
            assertThat(sampleEnds).containsExactly(23L, 35L, 1_001L).inOrder()
        }

    @Test
    fun rateLimitedLatencyAggregator_doesNotReportWhenNoInputs() =
        testScope.runTest {
            var numReports = 0
            val sampleStarts = mutableListOf<Long>()
            val sampleEnds = mutableListOf<Long>()

            val aggregator =
                RateLimitedLatencyAggregator.create(
                    period = 1.seconds,
                    testScope.backgroundScope,
                ) { startNanos: Long, endNanos: Long ->
                    numReports += 1
                    sampleStarts.add(startNanos)
                    sampleEnds.add(endNanos)
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                delay(0.5.seconds)
                aggregator.aggregate(20, 35)

                // A longer delay with no inputs should not result in a report.
                delay(3.seconds)
                aggregator.aggregate(40, 50)
            }

            testScope.advanceTimeBy(10.seconds)

            // The reporting callback should only have been called twice, with values for the two
            // non-empty reporting windows.
            assertThat(numReports).isEqualTo(2)
            assertThat(sampleStarts).containsExactly(20L, 40L).inOrder()
            assertThat(sampleEnds).containsExactly(35L, 50L).inOrder()
        }

    @Test
    fun rateLimitedLatencyAggregator_reportSynchronouslyReportsSynchronously() =
        testScope.runTest {
            val sampleStarts = mutableListOf<Long>()
            val sampleEnds = mutableListOf<Long>()

            // Use a reporting period of 10 seconds.
            val aggregator =
                RateLimitedLatencyAggregator.create(
                    period = 10.seconds,
                    testScope.backgroundScope,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts.add(startNanos)
                    sampleEnds.add(endNanos)
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                // Offset partially into the first reporting period to avoid precision and timing
                // issues.
                delay(1.seconds)

                // First window: 3 calls to `aggregate`.
                callAggregateWith(aggregator, 11L..13L, 21L..23L)
                delay(10.seconds)

                // Second window: 9 calls to `aggregate`, spread out over time.
                callAggregateWith(aggregator, 31L..33L, 41L..43L) // A. time=11
                delay(4.seconds)
                callAggregateWith(aggregator, 51L..53L, 61L..63L) // B. time=15
                delay(4.seconds)
                callAggregateWith(aggregator, 71L..73L, 81L..83L) // C. time=19
            }

            // Advance to time=12, partway into the second reporting window.
            testScope.advanceTimeBy(12.seconds)

            // Since only the first reporting window has completed, we have only one result.
            assertThat(sampleStarts).containsExactly(13L)
            assertThat(sampleEnds).containsExactly(23L)

            aggregator.reportSynchronously()

            // The manually-triggered report includes just call A.
            assertThat(sampleStarts).containsExactly(13L, 33L).inOrder()
            assertThat(sampleEnds).containsExactly(23L, 43L).inOrder()

            // Advance past the end of the second window.
            testScope.advanceTimeBy(10.seconds)

            // Calls B and C get included in the second window. Call A was already accounted for so
            // it
            // doesn't get double-counted.
            assertThat(sampleStarts).containsExactly(13L, 33L, 73L).inOrder()
            assertThat(sampleEnds).containsExactly(23L, 43L, 83L).inOrder()
        }

    @Test
    fun rateLimitedLatencyAggregator_canStartTwoAggregatorsInSameCoroutine() =
        testScope.runTest {
            val sampleStarts1 = mutableListOf<Long>()
            val sampleEnds1 = mutableListOf<Long>()
            val sampleStarts2 = mutableListOf<Long>()
            val sampleEnds2 = mutableListOf<Long>()

            val aggregator =
                RateLimitedLatencyAggregator.create(
                    period = 1.seconds,
                    testScope.backgroundScope,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts1.add(startNanos)
                    sampleEnds1.add(endNanos)
                }
            val otherAggregator =
                RateLimitedLatencyAggregator.create(
                    period = 1.seconds,
                    testScope.backgroundScope,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts2.add(startNanos)
                    sampleEnds2.add(endNanos)
                }

            // Feed input into the aggregators in a separate coroutine, so we can specify timing.
            launch {
                delay(0.5.seconds)
                aggregator.aggregate(10, 20)
                aggregator.aggregate(11, 21)
                otherAggregator.aggregate(12, 22)
                otherAggregator.aggregate(13, 23)

                delay(1.seconds)
                aggregator.aggregate(20, 35)

                delay(1.seconds)
                aggregator.aggregate(30, 40)
                aggregator.aggregate(31, 41)
                otherAggregator.aggregate(32, 42)
                aggregator.aggregate(33, 43)
                aggregator.aggregate(34, 44)
                aggregator.aggregate(35, 45)
                otherAggregator.aggregate(36, 46)
                aggregator.aggregate(37, 47)
                otherAggregator.aggregate(38, 48)
                aggregator.aggregate(39, 49)
            }

            testScope.advanceTimeBy(10.seconds)

            // The last input in each second should have been reported.
            assertThat(sampleStarts1).containsExactly(11L, 20L, 39L).inOrder()
            assertThat(sampleEnds1).containsExactly(21L, 35L, 49L).inOrder()
            assertThat(sampleStarts2).containsExactly(13L, 38L).inOrder()
            assertThat(sampleEnds2).containsExactly(23L, 48L).inOrder()
        }

    @Test
    fun rateLimitedLatencyAggregator_canStopOneAggregatorWhileTheOtherContinues() =
        testScope.runTest {
            val sampleStarts1 = mutableListOf<Long>()
            val sampleEnds1 = mutableListOf<Long>()
            val sampleStarts2 = mutableListOf<Long>()
            val sampleEnds2 = mutableListOf<Long>()

            val aggregator =
                RateLimitedLatencyAggregator.create(
                    period = 1.seconds,
                    testScope.backgroundScope,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts1.add(startNanos)
                    sampleEnds1.add(endNanos)
                }
            val otherAggregator =
                RateLimitedLatencyAggregator.create(
                    period = 1.seconds,
                    testScope.backgroundScope,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts2.add(startNanos)
                    sampleEnds2.add(endNanos)
                }

            // Feed input into the aggregators in a separate coroutine, so we can specify timing.
            launch {
                delay(0.5.seconds)
                aggregator.aggregate(10, 20)
                aggregator.aggregate(11, 21)
                otherAggregator.aggregate(12, 22)
                otherAggregator.aggregate(13, 23)

                delay(1.seconds)
                aggregator.aggregate(20, 35)
                otherAggregator.aggregate(21, 36)

                delay(1.seconds)
                aggregator.aggregate(30, 40)
                aggregator.aggregate(31, 41)
                otherAggregator.aggregate(32, 42)
                aggregator.aggregate(33, 43)
                aggregator.aggregate(34, 44)
                aggregator.aggregate(35, 45)
                otherAggregator.aggregate(36, 46)
                aggregator.aggregate(37, 47)
                otherAggregator.aggregate(38, 48)
                aggregator.aggregate(39, 49)
            }

            // Cancel the first aggregator's job just before the third batch is to be reported, but
            // after
            // it has received inputs for that batch.
            testScope.advanceTimeBy(2.75.seconds)
            aggregator.job().cancelAndJoin()

            // Keep running for a while to let everything play out for otherAggregator.
            testScope.advanceTimeBy(8.seconds)

            // aggregator stopped before the third batch was reported, so there are just two
            // results.
            assertThat(sampleStarts1).containsExactly(11L, 20L).inOrder()
            assertThat(sampleEnds1).containsExactly(21L, 35L).inOrder()

            // otherAggregator sent reports for all three batches.
            assertThat(sampleStarts2).containsExactly(13L, 21L, 38L).inOrder()
            assertThat(sampleEnds2).containsExactly(23L, 36L, 48L).inOrder()
        }

    @Test
    fun rateLimitedLatencyAggregator_cancelingScopeCancelsAggregator() =
        testScope.runTest {
            val sampleStarts = mutableListOf<Long>()
            val sampleEnds = mutableListOf<Long>()

            val aggregator =
                RateLimitedLatencyAggregator.create(
                    period = 1.seconds,
                    testScope.backgroundScope,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts.add(startNanos)
                    sampleEnds.add(endNanos)
                }

            // Feed input into the aggregator in a separate coroutine, so we can specify timing.
            launch {
                delay(0.5.seconds)
                callAggregateWith(aggregator, 10L..13L, 20L..23L)

                delay(1.seconds)
                aggregator.aggregate(20, 35)

                delay(1.seconds)
                callAggregateWith(aggregator, 30L..39L, 40L..49L)
            }

            // Cancel the background scope just before the third batch is to be reported.
            testScope.advanceTimeBy(2.75.seconds)
            testScope.backgroundScope.cancel()

            // Keep running for a while to let everything play out.
            testScope.advanceTimeBy(8.seconds)

            // The last input in each second should have been reported, so long as the scope the
            // aggregation was running in was still active. In this case, aggregator ignored the
            // third
            // batch.
            assertThat(sampleStarts).containsExactly(13L, 20L).inOrder()
            assertThat(sampleEnds).containsExactly(23L, 35L).inOrder()
        }

    @Test
    fun percentileLatencyAggregator_throwsOnInvalidFactoryParams() {
        assertFailsWith(IllegalStateException::class) {
            RateLimitedLatencyAggregator.create(
                period = 0.seconds, // Must be positive.
                testScope.backgroundScope,
            ) { _: Long, _: Long ->
            }
        }
    }

    /** A test helper for making many calls to [RateLimitedLatencyAggregator.aggregate]. */
    private fun callAggregateWith(
        aggregator: RateLimitedLatencyAggregator,
        starts: LongProgression,
        ends: LongProgression,
    ) {
        check(starts.count() == ends.count())
        for ((start, end) in starts.zip(ends)) {
            aggregator.aggregate(start, end)
        }
    }
}
