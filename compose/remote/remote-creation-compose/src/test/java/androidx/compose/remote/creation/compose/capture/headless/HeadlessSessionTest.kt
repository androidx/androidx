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

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HeadlessSessionTest {

    /** Content emits no nodes; tests observe state and effects only. */
    private object NoopApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) {}

        override fun insertBottomUp(index: Int, instance: Unit) {}

        override fun remove(index: Int, count: Int) {}

        override fun move(from: Int, to: Int, count: Int) {}

        override fun onClear() {}
    }

    /** A session on the test scheduler's virtual time, started before content is set. */
    private suspend fun TestScope.newSession(): HeadlessSession =
        HeadlessSession(
            parentContext = currentCoroutineContext(),
            name = "test",
            nanoTime = { testScheduler.currentTime * 1_000_000 },
            timeSource = testScheduler.timeSource,
        )

    private suspend fun HeadlessSession.setContent(content: @Composable () -> Unit): Composition {
        val composition = Composition(NoopApplier, recomposer)
        withContext(recomposerDispatcher) { composition.setContent(content) }
        return composition
    }

    // --- lifecycle -----------------------------------------------------------------------------

    @Test
    fun start_twice_throws() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            val failure = assertThrows(IllegalStateException::class.java) { session.start(this) }
            assertThat(failure).hasMessageThat().contains("Started")
            session.close()
        }
    }

    @Test
    fun awaitQuiescence_beforeStart_throws() = runTest {
        val session = newSession()
        val failure = runCatching { session.awaitQuiescence() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(failure).hasMessageThat().contains("Created")
        session.close()
    }

    @Test
    fun close_beforeStart_preventsStart() = runTest {
        val session = newSession()
        session.close()
        coroutineScope {
            val failure = assertThrows(IllegalStateException::class.java) { session.start(this) }
            assertThat(failure).hasMessageThat().contains("Closed")
        }
    }

    @Test
    fun close_isIdempotent_andEndsStartScope() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            session.close()
            session.close()
        }
        // The coroutineScope above returned, so the pump and the recomposer runner completed.
        assertThat(session.frameClock.hasAwaiters).isFalse()
    }

    @Test
    fun close_whileSettlingOnPacedFrames_cancelsTheWait() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            session.frameClock.minFramePeriod = 100.milliseconds
            session.setContent {
                LaunchedEffect(Unit) {
                    while (true) {
                        withFrameNanos {}
                    }
                }
            }
            val settling = async { session.awaitQuiescence(pacedFrames = true) }
            runCurrent()
            assertThat(settling.isActive).isTrue()

            session.close()
            runCurrent()

            assertThat(settling.isCancelled).isTrue()
        }
    }

    // --- quiescence ----------------------------------------------------------------------------

    @Test
    fun awaitQuiescence_runsEffectsAndSettles() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            var frames by mutableStateOf(0)
            session.setContent {
                LaunchedEffect(Unit) { repeat(3) { withFrameNanos { frames++ } } }
            }

            val result = session.awaitQuiescence()

            assertThat(result).isEqualTo(QuiescenceResult.Quiescent)
            assertThat(frames).isEqualTo(3)
            session.close()
        }
    }

    @Test
    fun awaitQuiescence_immediateFrames_ignoresPacing() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            session.frameClock.minFramePeriod = 100.milliseconds
            var frames by mutableStateOf(0)
            var composed = 0
            session.setContent {
                // Each frame callback writes state the composition reads, so after every frame
                // the recomposer itself awaits a frame to recompose (PendingWork): those frames
                // must be sent immediately too, not left to the paced pump.
                composed = frames
                LaunchedEffect(Unit) { repeat(3) { withFrameNanos { frames++ } } }
            }

            val result = session.awaitQuiescence(pacedFrames = false)

            assertThat(result).isEqualTo(QuiescenceResult.Quiescent)
            assertThat(composed).isEqualTo(3)
            assertThat(currentTime).isEqualTo(0)
            session.close()
        }
    }

    @Test
    fun awaitQuiescence_pacedFrames_waitsForThePumpAtMinFramePeriod() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            session.frameClock.minFramePeriod = 100.milliseconds
            var frames by mutableStateOf(0)
            session.setContent {
                LaunchedEffect(Unit) { repeat(3) { withFrameNanos { frames++ } } }
            }

            val result = session.awaitQuiescence(pacedFrames = true)

            assertThat(result).isEqualTo(QuiescenceResult.Quiescent)
            assertThat(frames).isEqualTo(3)
            // The first pump frame is immediate, the next two are paced.
            assertThat(currentTime).isEqualTo(200)
            assertThat(session.frameClock.framesSent).isEqualTo(3)
            session.close()
        }
    }

    @Test
    fun awaitQuiescence_pacedFrames_stillBoundedByFrameBudget() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            session.frameClock.minFramePeriod = 10.milliseconds
            session.setContent {
                LaunchedEffect(Unit) {
                    while (true) {
                        withFrameNanos {}
                    }
                }
            }

            val result = session.awaitQuiescence(pacedFrames = true)

            assertThat(result).isEqualTo(QuiescenceResult.ContinuousFrames)
            // The budget starts counting when settling starts; the pump may already have sent the
            // effect's first frame by then.
            assertThat(session.frameClock.framesSent).isIn(MAX_FRAMES + 1L..MAX_FRAMES + 2L)
            session.close()
        }
    }

    @Test
    fun awaitQuiescence_pacedFrames_stateWrittenInFrameCallbackIsRecomposed() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            session.frameClock.minFramePeriod = 50.milliseconds
            var seen = -1
            var counter by mutableStateOf(0)
            session.setContent {
                seen = counter
                LaunchedEffect(Unit) { repeat(2) { withFrameNanos { counter++ } } }
            }

            val result = session.awaitQuiescence(pacedFrames = true)

            assertThat(result).isEqualTo(QuiescenceResult.Quiescent)
            assertThat(seen).isEqualTo(2)
            session.close()
        }
    }
}
