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

@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.util.Log
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptureUpdateThrottleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        ShadowLog.reset()
    }

    @After
    fun tearDown() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
    }

    private fun TestScope.limiter(burst: Int, intervalMillis: Long): UpdateRateLimiter =
        UpdateRateLimiter(burst, intervalMillis.milliseconds, testScheduler.timeSource)

    private suspend fun UpdateRateLimiter.emit() {
        awaitPermit()
        onEmitted()
    }

    private fun throttleWarnings() =
        ShadowLog.getLogsForTag("CaptureRemoteDocument").filter { it.type == Log.WARN }

    /** A document settling through a few quick updates must not be delayed or logged. */
    @Test
    fun burstIsEmittedWithoutWaiting() = runTest {
        val limiter = limiter(burst = 3, intervalMillis = 1_000)

        repeat(3) { limiter.emit() }

        assertThat(currentTime).isEqualTo(0)
        assertThat(throttleWarnings()).isEmpty()
    }

    /** Once the burst is used up, sustained updates are limited to one per interval. */
    @Test
    fun afterBurst_emitsOncePerInterval() = runTest {
        val limiter = limiter(burst = 3, intervalMillis = 1_000)
        repeat(3) { limiter.emit() }

        limiter.emit()
        assertThat(currentTime).isEqualTo(1_000)
        limiter.emit()
        assertThat(currentTime).isEqualTo(2_000)
    }

    /** Content that goes quiet earns its burst back, so the next state change is not delayed. */
    @Test
    fun idleTime_restoresBurst() = runTest {
        val limiter = limiter(burst = 3, intervalMillis = 1_000)
        repeat(3) { limiter.emit() }

        delay(3_000)
        val resumedAt = currentTime
        repeat(3) { limiter.emit() }

        assertThat(currentTime).isEqualTo(resumedAt)
    }

    /**
     * Renders that do not emit (skipped or identical documents) must not use up the budget, since
     * they cost the players nothing.
     */
    @Test
    fun permitsWithoutEmission_doNotUseBudget() = runTest {
        val limiter = limiter(burst = 1, intervalMillis = 1_000)

        repeat(10) { limiter.awaitPermit() }

        assertThat(currentTime).isEqualTo(0)
    }

    /**
     * Throttling is logged so that content animating through recomposition is visible to the
     * developer, but at most once per minute so that it does not flood the log.
     */
    @Test
    fun throttling_logsAtMostOncePerMinute() = runTest {
        val limiter = limiter(burst = 1, intervalMillis = 1_000)
        limiter.emit()

        repeat(10) { limiter.emit() }
        assertThat(throttleWarnings()).hasSize(1)

        delay(60.seconds)
        limiter.emit()
        limiter.emit()
        assertThat(throttleWarnings()).hasSize(2)
        assertThat(throttleWarnings().last().msg).contains("throttled")
    }

    @Test
    fun rateLimited_rejectsInvalidArguments() {
        assertThat(runCatching { CaptureUpdateThrottle.rateLimited(0, 1_000) }.isFailure).isTrue()
        assertThat(runCatching { CaptureUpdateThrottle.rateLimited(1, 0) }.isFailure).isTrue()
    }

    /**
     * End to end: with the burst used up by the initial document, a state change is held back (and
     * logged) rather than emitted immediately, whereas [CaptureUpdateThrottle.None] emits it
     * straight away.
     */
    @Test
    fun captureRemoteDocument_rateLimited_holdsBackUpdateAndLogs() = runTest {
        val emitted = captureTwoStates(CaptureUpdateThrottle.rateLimited(1, 600_000))

        assertThat(emitted).isEqualTo(1)
        assertThat(throttleWarnings()).hasSize(1)
    }

    @Test
    fun captureRemoteDocument_none_emitsUpdateImmediately() = runTest {
        val emitted = captureTwoStates(CaptureUpdateThrottle.None)

        assertThat(emitted).isEqualTo(2)
        assertThat(throttleWarnings()).isEmpty()
    }

    /**
     * Collects a capture on [Dispatchers.Default] (so throttling waits in real time), changes state
     * once after the first document, and returns how many documents were emitted within a short
     * window.
     */
    private suspend fun captureTwoStates(throttle: CaptureUpdateThrottle): Int {
        val state = mutableStateOf("First")
        val emissions = AtomicInteger()
        return withContext(Dispatchers.Default) {
            val job = launch {
                captureRemoteDocument(
                        context = context,
                        creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                        updateThrottle = throttle,
                        coroutineContext = Dispatchers.Default,
                    ) {
                        RemoteText(state.value.rs)
                    }
                    .collect { emissions.incrementAndGet() }
            }
            withTimeout(10.seconds) {
                while (emissions.get() == 0) delay(1)
            }
            state.value = "Second"
            Snapshot.sendApplyNotifications()
            // Long enough for an unthrottled update to be emitted.
            withTimeout(10.seconds) {
                val deadline = System.nanoTime() + 1.seconds.inWholeNanoseconds
                while (System.nanoTime() < deadline && emissions.get() < 2) delay(1)
            }
            job.cancelAndJoin()
            emissions.get()
        }
    }
}
