/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.session

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InteractiveFrameClockTest {
    private lateinit var clock: InteractiveFrameClock

    companion object {
        private const val NANOSECONDS_PER_MILLISECOND = 1_000_000
        private const val NANOSECONDS_PER_SECOND = 1_000_000_000L
        private const val BASELINE_HZ = 5
        private const val MIN_BASELINE_PERIOD = NANOSECONDS_PER_SECOND / BASELINE_HZ
        private const val INTERACTIVE_HZ = 20
        private const val MIN_INTERACTIVE_PERIOD = NANOSECONDS_PER_SECOND / INTERACTIVE_HZ
        private const val INTERACTIVE_TIMEOUT = 5_000L
    }
    @Test
    fun sendFramesAtBaselineHz() = runTest {
        advanceTimeBy(System.currentTimeMillis())
        clock = InteractiveFrameClock(this, BASELINE_HZ, INTERACTIVE_HZ, INTERACTIVE_TIMEOUT) {
            currentTime * NANOSECONDS_PER_MILLISECOND
        }
        // awaiter1 will be sent immediately, awaiter2 & awaiter3 will be sent together at least
        // 1/5th of a second later.
        val awaiter1 = async { clock.withFrameNanos { it } }
        runCurrent()
        val awaiter2 = async { clock.withFrameNanos { it } }
        runCurrent()
        val awaiter3 = async { clock.withFrameNanos { it } }
        advanceUntilIdle()
        val frame1 = awaiter1.await()
        val frame2 = awaiter2.await()
        val frame3 = awaiter3.await()
        assertThat(frame2 - frame1).isEqualTo(MIN_BASELINE_PERIOD)
        assertThat(frame2).isEqualTo(frame3)
    }

    @Test
    fun sendFramesAtInteractiveHz() = runTest {
        advanceTimeBy(System.currentTimeMillis())
        clock = InteractiveFrameClock(this, BASELINE_HZ, INTERACTIVE_HZ, INTERACTIVE_TIMEOUT) {
            currentTime * NANOSECONDS_PER_MILLISECOND
        }
        launch { clock.startInteractive() }
        // awaiter1 will be sent immediately, awaiter2 & awaiter3 will be sent together at least
        // 1/20th of a second later.
        val awaiter1 = async { clock.withFrameNanos { it } }
        runCurrent()
        val awaiter2 = async { clock.withFrameNanos { it } }
        runCurrent()
        val awaiter3 = async { clock.withFrameNanos { it } }
        advanceUntilIdle()
        val frame1 = awaiter1.await()
        val frame2 = awaiter2.await()
        val frame3 = awaiter3.await()
        assertThat(frame2 - frame1).isEqualTo(MIN_INTERACTIVE_PERIOD)
        assertThat(frame2).isEqualTo(frame3)
    }

    @Test
    fun interactiveModeTimeout() = runTest {
        advanceTimeBy(System.currentTimeMillis())
        clock = InteractiveFrameClock(this, BASELINE_HZ, INTERACTIVE_HZ, INTERACTIVE_TIMEOUT) {
            currentTime * NANOSECONDS_PER_MILLISECOND
        }
        launch { clock.startInteractive() }
        yield()
        assertThat(clock.currentHz()).isEqualTo(INTERACTIVE_HZ)
        advanceTimeBy(INTERACTIVE_TIMEOUT / 2)
        assertThat(clock.currentHz()).isEqualTo(INTERACTIVE_HZ)
        advanceTimeBy(1 + INTERACTIVE_TIMEOUT / 2)
        assertThat(clock.currentHz()).isEqualTo(BASELINE_HZ)
    }

    @Test
    fun stopInteractive() = runTest {
        advanceTimeBy(System.currentTimeMillis())
        clock = InteractiveFrameClock(this, BASELINE_HZ, INTERACTIVE_HZ, INTERACTIVE_TIMEOUT) {
            currentTime * NANOSECONDS_PER_MILLISECOND
        }
        val interactiveJob = launch { clock.startInteractive() }
        yield()
        assertThat(clock.currentHz()).isEqualTo(INTERACTIVE_HZ)
        clock.stopInteractive()
        assertThat(interactiveJob.isCompleted)
        assertThat(clock.currentHz()).isEqualTo(BASELINE_HZ)
    }
}
