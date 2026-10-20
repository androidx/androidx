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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RenderLoopTest {

    private object NoopApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) {}

        override fun insertBottomUp(index: Int, instance: Unit) {}

        override fun remove(index: Int, count: Int) {}

        override fun move(from: Int, to: Int, count: Int) {}

        override fun onClear() {}
    }

    /** A started session with [content] set, plus a counter of how often content composed. */
    private class Host(val session: HeadlessSession, val composition: Composition) {
        var compositions = 0
    }

    private suspend fun TestScope.startHost(): Host {
        val session =
            HeadlessSession(
                parentContext = currentCoroutineContext(),
                name = "test",
                nanoTime = { testScheduler.currentTime * 1_000_000 },
                timeSource = testScheduler.timeSource,
            )
        val host = Host(session, Composition(NoopApplier, session.recomposer))
        return host
    }

    /** Writes like a host with a global write observer would: the write is applied right away. */
    private fun <T> MutableState<T>.write(value: T) {
        this.value = value
        Snapshot.sendApplyNotifications()
    }

    private suspend fun Host.setContent(content: @Composable () -> Unit) {
        session.awaitRunning()
        withContext(session.recomposerDispatcher) {
            composition.setContent {
                compositions++
                content()
            }
        }
    }

    // --- rendering ------------------------------------------------------------------------------

    @Test
    fun renderLoop_rendersOnce_untilStateChanges() = runTest {
        val state = mutableStateOf("a")
        val rendered = mutableListOf<String>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent { state.value }
            val loop = launch {
                host.session.renderLoop {
                    renderIfChanged { state.value }?.let { rendered += it }
                    true
                }
            }
            runCurrent()
            assertThat(rendered).containsExactly("a")

            state.write("b")
            runCurrent()
            assertThat(rendered).containsExactly("a", "b").inOrder()

            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderLoop_coalescesABurstOfWrites() = runTest {
        val state = mutableStateOf(0)
        val rendered = mutableListOf<Int>()
        var settles = 0
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent { state.value }
            val loop = launch {
                host.session.renderLoop(beforeSettle = { settles++ }) {
                    renderIfChanged { state.value }?.let { rendered += it }
                    true
                }
            }
            runCurrent()

            repeat(5) { state.write(it + 1) }
            runCurrent()

            // One iteration and one render for the burst, showing the last value: the signals
            // left by writes 2..5 were composed by that iteration and do not buy another.
            assertThat(rendered).containsExactly(0, 5).inOrder()
            assertThat(settles).isEqualTo(2)
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderIfChanged_skipsWhenNothingComposedChanged() = runTest {
        val unrelated = mutableStateOf(0)
        var renders = 0
        var skips = 0
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val loop = launch {
                host.session.renderLoop {
                    if (renderIfChanged { "r" } == null) skips++ else renders++
                    true
                }
            }
            runCurrent()
            assertThat(renders).isEqualTo(1)

            // A write to state the composition never read wakes the loop but renders nothing.
            unrelated.write(1)
            runCurrent()

            assertThat(renders).isEqualTo(1)
            assertThat(skips).isAtLeast(1)
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderIfChanged_reRendersWhenStateReadByTheRenderChanges() = runTest {
        val readOnlyByRender = mutableStateOf("x")
        val rendered = mutableListOf<String>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val loop = launch {
                host.session.renderLoop(observeRenderReads = true) {
                    renderIfChanged { readOnlyByRender.value }?.let { rendered += it }
                    true
                }
            }
            runCurrent()
            assertThat(rendered).containsExactly("x")

            readOnlyByRender.write("y")
            runCurrent()

            // No recomposition happened (the state is not read by content), yet the render ran.
            assertThat(host.compositions).isEqualTo(1)
            assertThat(rendered).containsExactly("x", "y").inOrder()
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderIfChanged_withoutReadObservation_doesNotReRenderForRenderReads() = runTest {
        val readOnlyByRender = mutableStateOf("x")
        val rendered = mutableListOf<String>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val loop = launch {
                host.session.renderLoop(observeRenderReads = false) {
                    renderIfChanged { readOnlyByRender.value }?.let { rendered += it }
                    true
                }
            }
            runCurrent()
            readOnlyByRender.write("y")
            runCurrent()

            assertThat(rendered).containsExactly("x")
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderIfChanged_rejectsWritesUnlessApplied() = runTest {
        val written = mutableStateOf(0)
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val failure = CompletableDeferred<Throwable?>()
            val loop = launch {
                failure.complete(
                    runCatching {
                        host.session.renderLoop {
                            renderIfChanged { written.value = 1 }
                            false
                        }
                    }
                        .exceptionOrNull()
                )
            }
            runCurrent()
            assertThat(failure.await()).isInstanceOf(IllegalStateException::class.java)
            assertThat(failure.await()).hasMessageThat().contains("written while rendering")
            loop.join()

            // With applyWrites the same write is applied after the render.
            host.session.renderLoop {
                renderIfChanged(applyWrites = true) { written.value = 2 }
                false
            }
            assertThat(written.value).isEqualTo(2)
            host.session.close()
        }
    }

    // --- loop control ---------------------------------------------------------------------------

    @Test
    fun renderLoop_runsBeforeSettleEveryIteration_andStopsWhenBodyReturnsFalse() = runTest {
        val state = mutableStateOf(0)
        var settles = 0
        var iterations = 0
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent { state.value }
            val loop = launch {
                host.session.renderLoop(beforeSettle = { settles++ }) {
                    renderIfChanged { state.value }
                    ++iterations < 2
                }
            }
            runCurrent()
            state.write(1)
            runCurrent()
            assertThat(loop.isCompleted).isTrue()
            assertThat(iterations).isEqualTo(2)
            assertThat(settles).isEqualTo(2)

            // Further writes do nothing once the loop returned.
            state.write(2)
            runCurrent()
            assertThat(iterations).isEqualTo(2)
            host.session.close()
        }
    }

    @Test
    fun renderLoop_endsWhenTheSessionIsClosed() = runTest {
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val loop = launch {
                host.session.renderLoop {
                    renderIfChanged { Unit }
                    true
                }
            }
            runCurrent()
            assertThat(loop.isActive).isTrue()

            host.session.close()
            runCurrent()

            assertThat(loop.isCompleted).isTrue()
            assertThat(loop.isCancelled).isFalse()
        }
    }

    @Test
    fun renderLoop_endsWhenTheSessionIsClosed_duringBeforeSettle() = runTest {
        val gate = CompletableDeferred<Unit>()
        var settles = 0
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val loop = launch {
                host.session.renderLoop(
                    beforeSettle = {
                        settles++
                        gate.await()
                    }
                ) {
                    renderIfChanged { Unit }
                    true
                }
            }
            runCurrent()
            assertThat(settles).isEqualTo(1)

            host.session.close()
            gate.complete(Unit)
            runCurrent()

            assertThat(loop.isCompleted).isTrue()
            assertThat(loop.isCancelled).isFalse()
        }
    }

    @Test
    fun renderLoop_endsWhenTheSessionIsClosed_whileSettlingOnPacedFrames() = runTest {
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.session.frameClock.minFramePeriod = 100.milliseconds
            host.setContent {
                LaunchedEffect(Unit) {
                    while (true) {
                        withFrameNanos {}
                    }
                }
            }
            val loop = launch {
                host.session.renderLoop(pacedFrames = true) {
                    renderIfChanged { Unit }
                    true
                }
            }
            runCurrent()
            // Settling is waiting for the pump's next paced frame.
            assertThat(loop.isActive).isTrue()

            host.session.close()
            runCurrent()

            assertThat(loop.isCompleted).isTrue()
            assertThat(loop.isCancelled).isFalse()
        }
    }

    @Test
    fun renderLoop_secondLoopOnTheSameSession_throws() = runTest {
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val loop = launch {
                host.session.renderLoop {
                    renderIfChanged { Unit }
                    true
                }
            }
            runCurrent()
            val failure = runCatching { host.session.renderLoop { false } }.exceptionOrNull()
            assertThat(failure).isInstanceOf(IllegalStateException::class.java)
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderLoop_propagatesFailuresFromTheBody() = runTest {
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val failure = runCatching {
                host.session.renderLoop { error("boom") }
            }
                .exceptionOrNull()
            assertThat(failure).hasMessageThat().isEqualTo("boom")
            // The loop released its registration, so another one can run.
            host.session.renderLoop { false }
            host.session.close()
        }
    }

    // --- cancellable renders --------------------------------------------------------------------

    @Test
    fun renderLoop_cancelRenderOnInvalidation_cancelsAStaleRenderAndRendersTheNewState() = runTest {
        val state = mutableStateOf("a")
        val rendered = mutableListOf<String>()
        val processed = mutableListOf<String>()
        var cancelled = 0
        val gate = CompletableDeferred<Unit>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent { state.value }
            val loop = launch {
                host.session.renderLoop(cancelRenderOnInvalidation = true) {
                    val value = renderIfChanged { state.value } ?: return@renderLoop true
                    rendered += value
                    try {
                        // A slow "processEmittableTree": the first one never finishes by itself.
                        if (value == "a") gate.await()
                        processed += value
                    } catch (e: CancellationException) {
                        cancelled++
                        throw e
                    }
                    true
                }
            }
            runCurrent()
            assertThat(rendered).containsExactly("a")
            assertThat(processed).isEmpty()

            state.write("b")
            runCurrent()

            assertThat(cancelled).isEqualTo(1)
            assertThat(rendered).containsExactly("a", "b").inOrder()
            assertThat(processed).containsExactly("b")
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderLoop_withoutCancellation_letsAStaleRenderFinish() = runTest {
        val state = mutableStateOf("a")
        val processed = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent { state.value }
            val loop = launch {
                host.session.renderLoop {
                    val value = renderIfChanged { state.value } ?: return@renderLoop true
                    if (value == "a") gate.await()
                    processed += value
                    true
                }
            }
            runCurrent()
            state.write("b")
            runCurrent()
            assertThat(processed).isEmpty()

            gate.complete(Unit)
            runCurrent()

            assertThat(processed).containsExactly("a", "b").inOrder()
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderLoop_cancelRenderOnInvalidation_closeEndsALongRender() = runTest {
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val loop = launch {
                host.session.renderLoop(cancelRenderOnInvalidation = true) {
                    renderIfChanged { Unit }
                    awaitCancellation()
                }
            }
            runCurrent()
            assertThat(loop.isActive).isTrue()

            host.session.close()
            runCurrent()

            assertThat(loop.isCompleted).isTrue()
            assertThat(loop.isCancelled).isFalse()
        }
    }

    @Test
    fun renderLoop_cancelRenderOnInvalidation_anUnrelatedWriteLetsTheRenderFinish() = runTest {
        val state = mutableStateOf("a")
        val unrelated = mutableStateOf(0)
        val processed = mutableListOf<String>()
        var cancelled = 0
        val gate = CompletableDeferred<Unit>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent { state.value }
            val loop = launch {
                host.session.renderLoop(cancelRenderOnInvalidation = true) {
                    val value = renderIfChanged { state.value } ?: return@renderLoop true
                    try {
                        gate.await()
                        processed += value
                    } catch (e: CancellationException) {
                        cancelled++
                        throw e
                    }
                    true
                }
            }
            runCurrent()

            // Written elsewhere in the process while the render is suspended: neither composition
            // nor the render read it, so the render is still current.
            unrelated.write(1)
            runCurrent()
            assertThat(cancelled).isEqualTo(0)

            gate.complete(Unit)
            runCurrent()
            assertThat(processed).containsExactly("a")
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderLoop_cancelRenderOnInvalidation_aWriteDuringSettleDoesNotCancelTheRender() = runTest {
        val state = mutableStateOf("a")
        var settles = 0
        val rendered = mutableListOf<String>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {
                // Applied while the loop settles, before the render starts: the render sees it.
                LaunchedEffect(Unit) { state.value = "b" }
                state.value
            }
            val loop = launch {
                host.session.renderLoop(
                    cancelRenderOnInvalidation = true,
                    beforeSettle = { settles++ },
                ) {
                    renderIfChanged { state.value }?.let { rendered += it }
                    true
                }
            }
            runCurrent()

            assertThat(rendered).containsExactly("b")
            assertThat(settles).isEqualTo(1)
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderLoop_cancelRenderOnInvalidation_aChangeToStateReadByTheRenderCancelsIt() = runTest {
        // Read by the render only, like state read inside a draw lambda: composition never changes.
        val renderInput = mutableStateOf("a")
        val processed = mutableListOf<String>()
        var cancelled = 0
        val gate = CompletableDeferred<Unit>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            val loop = launch {
                host.session.renderLoop(cancelRenderOnInvalidation = true) {
                    val value = renderIfChanged { renderInput.value } ?: return@renderLoop true
                    try {
                        if (value == "a") gate.await()
                        processed += value
                    } catch (e: CancellationException) {
                        cancelled++
                        throw e
                    }
                    true
                }
            }
            runCurrent()
            assertThat(processed).isEmpty()

            renderInput.write("b")
            runCurrent()

            assertThat(cancelled).isEqualTo(1)
            assertThat(processed).containsExactly("b")
            loop.cancel()
            host.session.close()
        }
    }

    // --- pacing ---------------------------------------------------------------------------------

    @Test
    fun renderLoop_pacedFrames_rendersAnimatingContentAtThePumpsPace() = runTest {
        var frames by mutableStateOf(0)
        val rendered = mutableListOf<Int>()
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.session.frameClock.minFramePeriod = 100.milliseconds
            host.setContent {
                LaunchedEffect(Unit) {
                    while (true) {
                        withFrameNanos { frames++ }
                    }
                }
                frames
            }
            val loop = launch {
                // maxFrames = 1: settle for at most one frame, then render whatever state is.
                host.session.renderLoop(pacedFrames = true, maxFrames = 1) {
                    renderIfChanged { frames }?.let { rendered += it }
                    true
                }
            }
            advanceTimeBy(1_000)
            runCurrent()

            // A budget of one frame allows one frame and gives up on the second, so each
            // iteration takes two paced frames: about five renders per second at 100ms.
            assertThat(rendered.size).isIn(4..6)
            assertThat(rendered).isInStrictOrder()
            loop.cancel()
            host.session.close()
        }
    }

    @Test
    fun renderLoop_pacedFrames_withCancellation_rendersAnimatingContentAtThePumpsPace() = runTest {
        var frames by mutableStateOf(0)
        val rendered = mutableListOf<Int>()
        var cancelled = 0
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.session.frameClock.minFramePeriod = 100.milliseconds
            host.setContent {
                LaunchedEffect(Unit) {
                    while (true) {
                        withFrameNanos { frames++ }
                    }
                }
                frames
            }
            val loop = launch {
                host.session.renderLoop(
                    pacedFrames = true,
                    maxFrames = 1,
                    cancelRenderOnInvalidation = true,
                ) {
                    val frame = renderIfChanged { frames } ?: return@renderLoop true
                    try {
                        yield()
                        rendered += frame
                    } catch (e: CancellationException) {
                        cancelled++
                        throw e
                    }
                    true
                }
            }
            advanceTimeBy(1_000)
            runCurrent()

            // The frames written while settling do not cancel the render they led to, so the
            // loop keeps the same pace as without cancellation.
            assertThat(cancelled).isEqualTo(0)
            assertThat(rendered.size).isIn(4..6)
            assertThat(rendered).isInStrictOrder()
            loop.cancel()
            host.session.close()
        }
    }

    // --- awaitRunning ---------------------------------------------------------------------------

    @Test
    fun awaitRunning_thenSetContent_composesOnce_onAnUnconfinedHost() = runTest {
        // On an unconfined (or non-immediate main) dispatcher the session keeps the caller's
        // interceptor, so withContext(recomposerDispatcher) runs inline, before the launched
        // recomposer. awaitRunning() closes that gap.
        val session =
            HeadlessSession(
                parentContext = Dispatchers.Unconfined + Job(),
                name = "unconfined",
                nanoTime = { System.nanoTime() },
                timeSource = kotlin.time.TimeSource.Monotonic,
            )
        var compositions = 0
        val composition = Composition(NoopApplier, session.recomposer)
        withContext(Dispatchers.Unconfined) {
            coroutineScope {
                session.start(this)
                session.awaitRunning()
                withContext(session.recomposerDispatcher) {
                    composition.setContent { compositions++ }
                }
                assertThat(session.awaitQuiescence()).isEqualTo(QuiescenceResult.Quiescent)
                assertThat(compositions).isEqualTo(1)
                session.close()
            }
        }
        composition.dispose()
    }

    @Test
    fun awaitQuiescence_onAnUnconfinedHost_settlesAnEffectWritingStateFromFrames() = runTest {
        // On an unconfined dispatcher nested resumptions (the effect starting, the recomposer and
        // the pump waking) queue in the thread's event loop; settling must drain it in place.
        val session =
            HeadlessSession(
                parentContext = Dispatchers.Unconfined + Job(),
                name = "unconfined",
                nanoTime = { System.nanoTime() },
                timeSource = kotlin.time.TimeSource.Monotonic,
            )
        var frames by mutableStateOf(0)
        var composed = -1
        val composition = Composition(NoopApplier, session.recomposer)
        withContext(Dispatchers.Unconfined) {
            coroutineScope {
                session.start(this)
                session.awaitRunning()
                withContext(session.recomposerDispatcher) {
                    composition.setContent {
                        composed = frames
                        LaunchedEffect(Unit) { repeat(3) { withFrameNanos { frames++ } } }
                    }
                }
                assertThat(session.awaitQuiescence()).isEqualTo(QuiescenceResult.Quiescent)
                assertThat(frames).isEqualTo(3)
                assertThat(composed).isEqualTo(3)
                session.close()
            }
        }
        composition.dispose()
    }

    @Test
    fun awaitRunning_beforeStart_throws() = runTest {
        val host = startHost()
        val failure = runCatching { host.session.awaitRunning() }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        host.session.close()
    }

    @Test
    fun invalidations_areSignalledForApplies_fromStart() = runTest {
        val state = mutableStateOf(0)
        val host = startHost()
        coroutineScope {
            host.session.start(this)
            host.setContent {}
            runCurrent()
            // Drain whatever start and setContent produced.
            while (host.session.invalidations.tryReceive().isSuccess) {}

            state.value = 1
            Snapshot.sendApplyNotifications()

            assertThat(host.session.invalidations.tryReceive().isSuccess).isTrue()
            assertThat(host.session.invalidations.tryReceive().isSuccess).isFalse()
            host.session.close()
        }
    }
}
