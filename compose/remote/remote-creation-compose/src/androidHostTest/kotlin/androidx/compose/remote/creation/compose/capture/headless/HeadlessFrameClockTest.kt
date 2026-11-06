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

package androidx.compose.remote.creation.compose.capture.headless

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test

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
    fun minFramePeriod_pacesPumpFrames() = runTest {
        val clock = HeadlessFrameClock(nanoTime = { now }, timeSource = testScheduler.timeSource)
        clock.minFramePeriod = 100.milliseconds
        val pump = launch { clock.runClock() }

        clock.withFrameNanos {}
        assertThat(currentTime).isEqualTo(0)
        clock.withFrameNanos {}
        assertThat(currentTime).isEqualTo(100)
        clock.withFrameNanos {}
        assertThat(currentTime).isEqualTo(200)

        pump.cancelAndJoin()
    }

    @Test
    fun minFramePeriod_zero_liftsPacing() = runTest {
        val clock = HeadlessFrameClock(nanoTime = { now }, timeSource = testScheduler.timeSource)
        clock.minFramePeriod = 100.milliseconds
        val pump = launch { clock.runClock() }
        clock.withFrameNanos {}

        clock.minFramePeriod = 0.milliseconds
        clock.withFrameNanos {}

        assertThat(currentTime).isEqualTo(0)
        pump.cancelAndJoin()
    }

    @Test
    fun minFramePeriod_doesNotPaceSendFrame() = runTest {
        val clock = HeadlessFrameClock(nanoTime = { now }, timeSource = testScheduler.timeSource)
        clock.minFramePeriod = 100.milliseconds
        val pump = launch { clock.runClock() }
        clock.withFrameNanos {}
        val frame = async { clock.withFrameNanos { it } }
        runCurrent()

        clock.sendFrame()
        runCurrent()

        assertThat(frame.isCompleted).isTrue()
        assertThat(currentTime).isEqualTo(0)
        pump.cancelAndJoin()
    }

    @Test
    fun awaitFrame_resumesAfterPumpFrame_pacedByMinFramePeriod() = runTest {
        val clock = HeadlessFrameClock(nanoTime = { now }, timeSource = testScheduler.timeSource)
        clock.minFramePeriod = 100.milliseconds
        val pump = launch { clock.runClock() }
        clock.withFrameNanos {}
        val awaiter = launch { clock.withFrameNanos {} }

        clock.awaitFrame()

        assertThat(awaiter.isCompleted).isTrue()
        assertThat(currentTime).isEqualTo(100)
        assertThat(clock.framesSent).isEqualTo(2)
        pump.cancelAndJoin()
    }

    @Test
    fun awaitFrame_doesNotResumeForFramesSentBeforeTheCall() = runTest {
        val pump = launch { clock.runClock() }
        clock.withFrameNanos {}
        assertThat(clock.framesSent).isEqualTo(1)

        val waiting = async { clock.awaitFrame() }
        runCurrent()
        assertThat(waiting.isCompleted).isFalse()

        clock.withFrameNanos {}
        runCurrent()
        assertThat(waiting.isCompleted).isTrue()
        pump.cancelAndJoin()
    }

    @Test
    fun awaitFrame_isCancelledByAnExhaustedBudget() = runTest {
        val pump = launch { clock.runClock() }
        val content = launch {
            while (true) {
                clock.withFrameNanos {}
            }
        }

        val result =
            clock.withFrameBudget(maxFrames = 3) {
                while (true) {
                    clock.awaitFrame()
                }
            }

        assertThat(result).isNull()
        content.cancelAndJoin()
        pump.cancelAndJoin()
    }

    @Test
    fun awaitFrame_throwsWhenTheClockIsCancelled() = runTest {
        val pump = launch { clock.runClock() }
        val waiting = async { clock.awaitFrame() }
        runCurrent()
        assertThat(waiting.isCompleted).isFalse()

        clock.cancel()
        runCurrent()

        assertThat(waiting.isCancelled).isTrue()
        assertThat(waiting.getCompletionExceptionOrNull())
            .hasMessageThat()
            .contains("HeadlessFrameClock was cancelled")
        pump.join()
    }

    @Test
    fun awaitFrame_afterCancel_throws() = runTest {
        clock.cancel()
        val failure = runCatching { clock.awaitFrame() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun awaitFrame_resumesWhenTheAwaiterIsCancelledWhileThePumpIsPacing() = runTest {
        val clock = HeadlessFrameClock(nanoTime = { now }, timeSource = testScheduler.timeSource)
        clock.minFramePeriod = 100.milliseconds
        val pump = launch { clock.runClock() }
        clock.withFrameNanos {}
        // Requests a frame, so the pump starts pacing, then gives up before it is sent.
        val awaiter = launch { clock.withFrameNanos {} }
        val waiting = async { clock.awaitFrame() }
        runCurrent()
        awaiter.cancelAndJoin()

        advanceTimeBy(100)
        runCurrent()

        assertThat(waiting.isCompleted).isTrue()
        assertThat(clock.hasAwaiters).isFalse()
        assertThat(clock.framesSent).isEqualTo(1)
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
    fun exhaustedBudget_resumedOnUnconfinedDispatcher_appliesNewBudgetBeforeWakingPump() = runTest {
        val clock = HeadlessFrameClock(nanoTime = { now })
        val pump = launch(Dispatchers.Unconfined) { clock.runClock() }
        var frames = 0
        var anim: Job? = null
        assertThat(
                clock.withFrameBudget(maxFrames = 2) {
                    anim =
                        this@runTest.launch(Dispatchers.Unconfined) {
                            while (frames < 10) {
                                clock.withFrameNanos { frames++ }
                            }
                        }
                    while (true) {
                        clock.awaitFrame()
                    }
                }
            )
            .isNull()
        assertThat(frames).isEqualTo(3)

        // Calling withFrameBudget from outside an active unconfined event loop resumes the parked
        // Unconfined pump inline inside requestFrame().
        val second =
            withContext(Dispatchers.Default) {
                clock.withFrameBudget(maxFrames = 2) {
                    while (frames < 10) {
                        clock.awaitFrame()
                    }
                }
            }

        assertThat(second).isNull()
        assertThat(frames).isEqualTo(6)
        anim?.cancelAndJoin()
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
