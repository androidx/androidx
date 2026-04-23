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
import kotlin.random.Random
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalLatencyDataApi::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // Required for `kotlin.time.Duration`
class FixedProbabilityLatencyAggregatorTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockRandom = mock<Random>()

    @Test
    fun fixedProbabilityLatencyAggregator_reportsOnlyWhenRandomNumberIsLowEnough() =
        testScope.runTest {
            val sampleStarts = mutableListOf<Long>()
            val sampleEnds = mutableListOf<Long>()

            val aggregator =
                FixedProbabilityLatencyAggregator.create(
                    sampleProbability = 0.3f,
                    testScope.backgroundScope,
                    mockRandom,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts.add(startNanos)
                    sampleEnds.add(endNanos)
                }

            // When the "random" number is above sampleProbability, no report happens.
            whenever(mockRandom.nextFloat()).thenReturn(0.5f)
            aggregator.aggregate(11L, 15L)

            // When it's below the sampleProbability, the report callback is called.
            whenever(mockRandom.nextFloat()).thenReturn(0.2f)
            aggregator.aggregate(21L, 25L)

            // Try again with a high number. No report happens.
            whenever(mockRandom.nextFloat()).thenReturn(0.4f)
            aggregator.aggregate(31L, 35L)

            // Wait for all running reports to complete.
            aggregator.job().cancelAndJoin()

            // Only one of the three `aggregate` calls resulted in a report.
            assertThat(sampleStarts).containsExactly(21L)
            assertThat(sampleEnds).containsExactly(25L)
        }

    @Test
    fun fixedProbabilityLatencyAggregator_reportSynchronouslyDoesNothing() =
        testScope.runTest {
            val sampleStarts = mutableListOf<Long>()
            val sampleEnds = mutableListOf<Long>()

            val aggregator =
                FixedProbabilityLatencyAggregator.create(
                    sampleProbability = 0.3f,
                    testScope.backgroundScope,
                    mockRandom,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts.add(startNanos)
                    sampleEnds.add(endNanos)
                }

            // When the "random" number is above sampleProbability, no report happens.
            whenever(mockRandom.nextFloat()).thenReturn(0.5f)
            aggregator.aggregate(11L, 15L)

            // Give the report time to run if it is actually triggered.
            delay(1.seconds)

            // reportSynchronously is a no-op.
            aggregator.reportSynchronously()

            // When it's below the sampleProbability, the report callback is called.
            whenever(mockRandom.nextFloat()).thenReturn(0.2f)
            aggregator.aggregate(21L, 25L)

            // Give the report time to run.
            delay(1.seconds)

            // reportSynchronously is a no-op in this case too.
            aggregator.reportSynchronously()

            // Wait for all running reports to complete.
            aggregator.job().cancelAndJoin()

            // Only one of the `aggregate` calls resulted in a report.
            assertThat(sampleStarts).containsExactly(21L)
            assertThat(sampleEnds).containsExactly(25L)
        }

    @Test
    fun fixedProbabilityLatencyAggregator_canStartTwoAggregatorsInSameCoroutine() =
        testScope.runTest {
            val sampleStarts1 = mutableListOf<Long>()
            val sampleEnds1 = mutableListOf<Long>()
            val sampleStarts2 = mutableListOf<Long>()
            val sampleEnds2 = mutableListOf<Long>()

            val aggregator =
                FixedProbabilityLatencyAggregator.create(
                    sampleProbability = 0.3f,
                    testScope.backgroundScope,
                    mockRandom,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts1.add(startNanos)
                    sampleEnds1.add(endNanos)
                }
            val otherAggregator =
                FixedProbabilityLatencyAggregator.create(
                    sampleProbability = 0.3f,
                    testScope.backgroundScope,
                    mockRandom,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts2.add(startNanos)
                    sampleEnds2.add(endNanos)
                }

            whenever(mockRandom.nextFloat()).thenReturn(0.5f) // High value => no report
            aggregator.aggregate(11L, 15L)
            whenever(mockRandom.nextFloat()).thenReturn(0.1f) // Low value => report
            otherAggregator.aggregate(11L, 15L)

            whenever(mockRandom.nextFloat()).thenReturn(0.2f) // Low value => report
            aggregator.aggregate(21L, 25L)
            whenever(mockRandom.nextFloat()).thenReturn(0.5f) // High value => no report
            otherAggregator.aggregate(21L, 25L)

            whenever(mockRandom.nextFloat()).thenReturn(0.4f) // High value => no report
            aggregator.aggregate(31L, 35L)
            whenever(mockRandom.nextFloat()).thenReturn(0.2f) // Low value => report
            otherAggregator.aggregate(31L, 35L)

            whenever(mockRandom.nextFloat()).thenReturn(0.2f) // Low value => report
            aggregator.aggregate(41L, 45L)
            whenever(mockRandom.nextFloat()).thenReturn(0.2f) // Low value => report
            otherAggregator.aggregate(41L, 45L)

            // Wait for all running reports to complete.
            aggregator.job().cancelAndJoin()
            otherAggregator.job().cancelAndJoin()

            assertThat(sampleStarts1).containsExactly(21L, 41L).inOrder()
            assertThat(sampleEnds1).containsExactly(25L, 45L).inOrder()
            assertThat(sampleStarts2).containsExactly(11L, 31L, 41L).inOrder()
            assertThat(sampleEnds2).containsExactly(15L, 35L, 45L).inOrder()
        }

    @Test
    fun fixedProbabilityLatencyAggregator_canStopOneAggregatorWhileTheOtherContinues() =
        testScope.runTest {
            val sampleStarts1 = mutableListOf<Long>()
            val sampleEnds1 = mutableListOf<Long>()
            val sampleStarts2 = mutableListOf<Long>()
            val sampleEnds2 = mutableListOf<Long>()

            val aggregator =
                FixedProbabilityLatencyAggregator.create(
                    sampleProbability = 0.3f,
                    testScope.backgroundScope,
                    mockRandom,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts1.add(startNanos)
                    sampleEnds1.add(endNanos)
                }
            val otherAggregator =
                FixedProbabilityLatencyAggregator.create(
                    sampleProbability = 0.3f,
                    testScope.backgroundScope,
                    mockRandom,
                ) { startNanos: Long, endNanos: Long ->
                    sampleStarts2.add(startNanos)
                    sampleEnds2.add(endNanos)
                }

            // Feed input into the aggregators in a separate coroutine, so we can specify timing.
            launch {
                delay(0.5.seconds)
                whenever(mockRandom.nextFloat()).thenReturn(0.5f) // High value => no report
                aggregator.aggregate(11L, 15L)
                whenever(mockRandom.nextFloat()).thenReturn(0.1f) // Low value => report
                otherAggregator.aggregate(11L, 15L)

                delay(1.seconds)
                whenever(mockRandom.nextFloat()).thenReturn(0.2f) // Low value => report
                aggregator.aggregate(21L, 25L)
                whenever(mockRandom.nextFloat()).thenReturn(0.5f) // High value => no report
                otherAggregator.aggregate(21L, 25L)

                // Below we stop `aggregator`'s job at this time, so no more reports should happen
                // regardless of the random number generator's behavior.

                delay(1.seconds)
                whenever(mockRandom.nextFloat()).thenReturn(0.4f) // High value => no report
                aggregator.aggregate(31L, 35L)
                whenever(mockRandom.nextFloat()).thenReturn(0.2f) // Low value => report
                otherAggregator.aggregate(31L, 35L)

                delay(1.seconds)
                whenever(mockRandom.nextFloat()).thenReturn(0.2f) // Low value => report
                aggregator.aggregate(41L, 45L)
                whenever(mockRandom.nextFloat()).thenReturn(0.2f) // Low value => report
                otherAggregator.aggregate(41L, 45L)
            }

            // Cancel the first aggregator's job after the second batch has been reported.
            testScope.advanceTimeBy(2.25.seconds)
            aggregator.job().cancelAndJoin()

            // Keep running for a while to let everything play out for otherAggregator.
            testScope.advanceTimeBy(10.seconds)
            otherAggregator.job().cancelAndJoin()

            assertThat(sampleStarts1).containsExactly(21L)
            assertThat(sampleEnds1).containsExactly(25L)
            assertThat(sampleStarts2).containsExactly(11L, 31L, 41L).inOrder()
            assertThat(sampleEnds2).containsExactly(15L, 35L, 45L).inOrder()
        }

    @Test
    fun fixedProbabilityLatencyAggregator_throwsOnInvalidFactoryParams() {
        assertFailsWith(IllegalStateException::class) {
            FixedProbabilityLatencyAggregator.create(
                sampleProbability = -0.1f, // Must be non-negative.
                testScope.backgroundScope,
            ) { _: Long, _: Long ->
            }
        }
        assertFailsWith(IllegalStateException::class) {
            FixedProbabilityLatencyAggregator.create(
                sampleProbability = 1.1f, // Must be <= 1.0.
                testScope.backgroundScope,
            ) { _: Long, _: Long ->
            }
        }
    }
}
