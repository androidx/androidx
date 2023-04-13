/*
 * Copyright 2023 The Android Open Source Project
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WithTimerTest {
    private val TestScope.timeSource
        get() = TimeSource { currentTime }

    @Test
    fun completingNormallyThrowsNoException() = runTest {
        val result = async {
            withTimerOrNull(timeSource) {
                startTimer(200.milliseconds)
                delay(100.milliseconds)
                Any()
            }
        }
        assertThat(result.await()).isNotNull()
    }

    @Test
    fun completingNormallyDoesNotCancelParentJob() = runTest {
        val timerSuccess = Job()
        val parentJob = launch {
            withTimer(timeSource) {
                startTimer(200.milliseconds)
                delay(100.milliseconds)
            }
            timerSuccess.complete()
            delay(Duration.INFINITE)
        }
        timerSuccess.join()
        assertThat(timerSuccess.isCompleted).isTrue()
        assertThat(parentJob.isActive).isTrue()
        parentJob.cancel()
    }

    @Test
    fun timeoutThrowsTimeoutCancellationException() = runTest {
        var expected: TimeoutCancellationException? = null
        try {
            withTimer(timeSource) {
                startTimer(200.milliseconds)
                delay(1.seconds)
            }
        } catch (e: TimeoutCancellationException) {
            expected = e
        } finally {
            assertThat(expected).isNotNull()
        }
    }

    @Test
    fun doesNotTimeoutBeforeStartTimer() = runTest {
        var unexpected: TimeoutCancellationException? = null
        try {
            withTimer(timeSource) {
                delay(30.days)
                startTimer(200.milliseconds)
                delay(100.milliseconds)
            }
        } catch (e: TimeoutCancellationException) {
            unexpected = e
        } finally {
            assertThat(unexpected).isNull()
        }
    }

    @Test
    fun addTime() = runTest {
        var unexpected: TimeoutCancellationException? = null
        try {
            withTimer(timeSource) {
                startTimer(200.milliseconds)
                delay(100)
                assertThat(timeLeft).isEqualTo(100.milliseconds)
                addTime(100.milliseconds)
                assertThat(timeLeft).isEqualTo(200.milliseconds)
                delay(199.milliseconds)
            }
        } catch (e: TimeoutCancellationException) {
            unexpected = e
        } finally {
            assertThat(unexpected).isNull()
        }
    }

    @Test
    fun addTimeBeforeStartTimer() = runTest {
        var expected: IllegalStateException? = null
        try {
            withTimer(timeSource) {
                addTime(100.milliseconds)
            }
        } catch (e: IllegalStateException) {
            expected = e
        } finally {
            assertThat(expected).isNotNull()
        }
    }

    @Test
    fun addNegativeDuration() = runTest {
        var expected: IllegalArgumentException? = null
        try {
            withTimer(timeSource) {
                startTimer(100.milliseconds)
                addTime(-100.milliseconds)
            }
        } catch (e: IllegalArgumentException) {
            expected = e
        } finally {
            assertThat(expected).isNotNull()
        }
    }

    @Test
    fun nestedWithTimerOrNull() = runTest {
        val checkpoint1 = AtomicBoolean(false)
        val checkpoint2 = AtomicBoolean(false)
        // The remaining two checkpoints should never be hit
        val checkpoint3 = AtomicBoolean(false)
        val checkpoint4 = AtomicBoolean(false)

        val result = withTimerOrNull(timeSource) {
            // The outer timer should trigger during the delay(), and nothing after that should run.
            startTimer(200.milliseconds)
            checkpoint1.set(true)
            withTimerOrNull(timeSource) {
                startTimer(1.seconds)
                checkpoint2.set(true)
                delay(201.milliseconds)
                checkpoint3.set(true)
            }
            checkpoint4.set(true)
        }

        assertThat(result).isNull()
        assertThat(checkpoint1.get()).isTrue()
        assertThat(checkpoint2.get()).isTrue()
        assertThat(checkpoint3.get()).isFalse()
        assertThat(checkpoint4.get()).isFalse()
    }
}
