/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.compose.remote.creation.compose.capture

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HeadlessFrameClockTest {

    private var now = 0L
    private val framesSent = AtomicInteger()
    private val clock = HeadlessFrameClock(nanoTime = { now }) { framesSent.incrementAndGet() }

    @Test
    fun pump_sendsFrameWhenRequested_withClockTime() = runTest {
        val pump = launch { clock.runClock() }

        now = 42
        val frameTime = clock.withFrameNanos { it }

        assertThat(frameTime).isEqualTo(42)
        assertThat(framesSent.get()).isEqualTo(1)
        assertThat(clock.framesSent).isEqualTo(1)
        pump.cancelAndJoin()
    }

    @Test
    fun frameRequestedBeforePumpStarts_isSentOncePumped() = runTest {
        val frame = async { clock.withFrameNanos { it } }
        runCurrent()
        assertThat(frame.isCompleted).isFalse()

        val pump = launch { clock.runClock() }
        now = 7

        assertThat(frame.await()).isEqualTo(7)
        pump.cancelAndJoin()
    }

    @Test
    fun sendFrame_withoutAwaiters_isNotCounted() = runTest {
        clock.sendFrame()

        assertThat(clock.framesSent).isEqualTo(0)
        assertThat(framesSent.get()).isEqualTo(0)
    }

    @Test
    fun sendFrame_resumesAwaiterSynchronously() = runTest {
        val frame = async { clock.withFrameNanos { it } }
        runCurrent()
        assertThat(clock.hasAwaiters).isTrue()

        now = 3
        clock.sendFrame()

        assertThat(clock.hasAwaiters).isFalse()
        assertThat(frame.await()).isEqualTo(3)
        assertThat(clock.framesSent).isEqualTo(1)
    }

    @Test
    fun withFrameBudget_returnsNullOnceExhausted() = runTest {
        val pump = launch { clock.runClock() }

        val result =
            clock.withFrameBudget(maxFrames = 3) {
                while (true) {
                    clock.withFrameNanos {}
                }
            }

        assertThat(result).isNull()
        assertThat(clock.framesSent).isEqualTo(4)
        pump.cancelAndJoin()
    }

    @Test
    fun withFrameBudget_returnsResultWhenWithinBudget() = runTest {
        val pump = launch { clock.runClock() }

        val result = clock.withFrameBudget(maxFrames = 5) { repeat(3) { clock.withFrameNanos {} } }

        assertThat(result).isEqualTo(Unit)
        pump.cancelAndJoin()
    }

    @Test
    fun withFrameBudget_rethrowsExternalCancellation() = runTest {
        val job = launch { clock.withFrameBudget(maxFrames = 5) { awaitCancellation() } }
        runCurrent()

        job.cancelAndJoin()

        assertThat(job.isCancelled).isTrue()
    }

    @Test
    fun cancel_failsAwaiters() = runTest {
        val frame = async { clock.withFrameNanos { it } }
        runCurrent()
        assertThat(clock.hasAwaiters).isTrue()

        clock.cancel()

        val failure = runCatching { frame.await() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(CancellationException::class.java)
        assertThat(clock.hasAwaiters).isFalse()
    }

    @Test
    fun cancel_stopsParkedPump() = runTest {
        val pump = launch { clock.runClock() }
        runCurrent()
        assertThat(pump.isActive).isTrue()

        clock.cancel()

        pump.join()
        assertThat(pump.isCancelled).isTrue()
    }

    @Test
    fun runClock_afterCancel_returnsImmediately() = runTest {
        clock.cancel()

        clock.runClock()
    }

    @Test
    fun runClock_twice_throws() = runTest {
        val pump = launch { clock.runClock() }
        runCurrent()

        val failure = runCatching { clock.runClock() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        pump.cancelAndJoin()
    }

    @Test
    fun exhaustedBudget_pausesPumpUntilNextBudget() = runTest {
        val pump = launch { clock.runClock() }
        val exhausted =
            clock.withFrameBudget(maxFrames = 2) {
                while (true) {
                    clock.withFrameNanos {}
                }
            }
        assertThat(exhausted).isNull()
        val sentWhenExhausted = clock.framesSent

        // Content keeps requesting frames, but the pump no longer serves them.
        val frame = async { clock.withFrameNanos { it } }
        runCurrent()
        assertThat(frame.isCompleted).isFalse()
        assertThat(clock.framesSent).isEqualTo(sentWhenExhausted)

        // The next budget resumes the pump, which serves the waiting awaiter.
        clock.withFrameBudget(maxFrames = 5) { clock.withFrameNanos {} }
        assertThat(frame.isCompleted).isTrue()
        pump.cancelAndJoin()
    }

    @Test
    fun cancellingPump_cancelsClock() = runTest {
        val pump = launch { clock.runClock() }
        runCurrent()

        pump.cancelAndJoin()

        val failure = runCatching { clock.withFrameNanos {} }.exceptionOrNull()
        assertThat(failure).isInstanceOf(CancellationException::class.java)
    }
}
