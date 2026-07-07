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

package androidx.ink.authoring.latency.aggregators.internal

import androidx.ink.authoring.ExperimentalInkLatencyDataApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalInkLatencyDataApi::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class ConcurrencyHelpersTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun runEvery_runsAtExpectedTimes() =
        testScope.runTest {
            var runCount = 0
            testScope.backgroundScope.runEvery(5.seconds) { runCount += 1 }

            // The lambda does not run immediately; the first run is after 5 seconds.

            // t=4.9
            testScope.advanceTimeBy(4.9.seconds)
            assertThat(runCount).isEqualTo(0)

            // t=5.1
            testScope.advanceTimeBy(0.2.seconds)
            assertThat(runCount).isEqualTo(1)

            // It runs again 5 seconds later.

            // t=9.9
            testScope.advanceTimeBy(4.8.seconds)
            assertThat(runCount).isEqualTo(1)

            // t=10.1
            testScope.advanceTimeBy(0.2.seconds)
            assertThat(runCount).isEqualTo(2)

            // And again 5 seconds after that.

            // t=14.9
            testScope.advanceTimeBy(4.8.seconds)
            assertThat(runCount).isEqualTo(2)

            // t=15.1
            testScope.advanceTimeBy(0.2.seconds)
            assertThat(runCount).isEqualTo(3)
        }

    @Test
    fun runEvery_twoCallsRunTwoPeriodicCallbacks() =
        testScope.runTest {
            var runCount3 = 0
            var runCount5 = 0
            testScope.backgroundScope.runEvery(3.seconds) { runCount3 += 1 }
            testScope.backgroundScope.runEvery(5.seconds) { runCount5 += 1 }

            // The lambdas do not run immediately; they first run after 5 and 3 seconds,
            // respectively.

            // t=2.9
            testScope.advanceTimeBy(2.9.seconds)
            assertThat(runCount3).isEqualTo(0)
            assertThat(runCount5).isEqualTo(0)

            // t=3.1
            testScope.advanceTimeBy(0.2.seconds)
            assertThat(runCount3).isEqualTo(1)
            assertThat(runCount5).isEqualTo(0)

            // t=4.9
            testScope.advanceTimeBy(1.8.seconds)
            assertThat(runCount3).isEqualTo(1)
            assertThat(runCount5).isEqualTo(0)

            // t=5.1
            testScope.advanceTimeBy(0.2.seconds)
            assertThat(runCount3).isEqualTo(1)
            assertThat(runCount5).isEqualTo(1)

            // t=5.9
            testScope.advanceTimeBy(0.8.seconds)
            assertThat(runCount3).isEqualTo(1)
            assertThat(runCount5).isEqualTo(1)

            // t=6.1
            testScope.advanceTimeBy(0.2.seconds)
            assertThat(runCount3).isEqualTo(2)
            assertThat(runCount5).isEqualTo(1)

            // t=8.9
            testScope.advanceTimeBy(2.8.seconds)
            assertThat(runCount3).isEqualTo(2)
            assertThat(runCount5).isEqualTo(1)

            // t=9.1
            testScope.advanceTimeBy(0.2.seconds)
            assertThat(runCount3).isEqualTo(3)
            assertThat(runCount5).isEqualTo(1)

            // t=9.9
            testScope.advanceTimeBy(0.8.seconds)
            assertThat(runCount3).isEqualTo(3)
            assertThat(runCount5).isEqualTo(1)

            // t=10.1
            testScope.advanceTimeBy(0.2.seconds)
            assertThat(runCount3).isEqualTo(3)
            assertThat(runCount5).isEqualTo(2)
        }

    @Test
    fun runEvery_cancelingScopeCancelsAllRunEveryLoops() =
        testScope.runTest {
            var runCount3 = 0
            var runCount5 = 0
            testScope.backgroundScope.runEvery(3.seconds) { runCount3 += 1 }
            testScope.backgroundScope.runEvery(5.seconds) { runCount5 += 1 }

            testScope.advanceTimeBy(8.9.seconds)
            assertThat(runCount3).isEqualTo(2)
            assertThat(runCount5).isEqualTo(1)

            testScope.backgroundScope.cancel()

            // After canceling, no more lambda calls happen.
            testScope.advanceTimeBy(20.seconds)
            assertThat(runCount3).isEqualTo(2)
            assertThat(runCount5).isEqualTo(1)
        }

    @Test
    fun runEvery_cancelingOneLoopDoesNotAffectTheOther() =
        testScope.runTest {
            var runCount3 = 0
            var runCount5 = 0
            val job3 = testScope.backgroundScope.runEvery(3.seconds) { runCount3 += 1 }
            testScope.backgroundScope.runEvery(5.seconds) { runCount5 += 1 }

            testScope.advanceTimeBy(8.9.seconds)
            assertThat(runCount3).isEqualTo(2)
            assertThat(runCount5).isEqualTo(1)

            job3.cancel()

            // t=15.1. After canceling job3, runCount3 ceases updating but runCount5 keeps going.
            testScope.advanceTimeBy(6.2.seconds)
            assertThat(runCount3).isEqualTo(2)
            assertThat(runCount5).isEqualTo(3)
        }
}
