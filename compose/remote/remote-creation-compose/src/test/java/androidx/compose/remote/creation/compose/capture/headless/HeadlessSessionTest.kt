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
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertThrows
import org.junit.Test

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
    fun close_whileAwaitingRunning_cancelsTheWait() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            val awaiting = async(start = CoroutineStart.UNDISPATCHED) { session.awaitRunning() }
            try {
                assertThat(awaiting.isActive).isTrue()

                session.close()
                runCurrent()

                assertThat(awaiting.isCancelled).isTrue()
            } finally {
                awaiting.cancel()
            }
        }
    }

    @Test
    fun awaitRunning_registersInvalidationObserverBeforeReturning() = runTest {
        val session = newSession()
        val state = mutableStateOf(0)
        coroutineScope {
            session.start(this)
            val signalledImmediatelyAfterRunning =
                async(Dispatchers.Unconfined) {
                    session.awaitRunning()
                    while (session.invalidations.tryReceive().isSuccess) {}
                    state.value = 1
                    Snapshot.sendApplyNotifications()
                    session.invalidations.tryReceive().isSuccess
                }
            runCurrent()

            assertThat(signalledImmediatelyAfterRunning.await()).isTrue()
            session.close()
        }
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

    @Test
    fun cancellingStartScope_whileAwaitingRunning_cancelsTheWait() = runTest {
        val session = newSession()
        val childScope = CoroutineScope(currentCoroutineContext() + Job())
        session.start(childScope)
        val awaiting = async(start = CoroutineStart.UNDISPATCHED) { session.awaitRunning() }
        assertThat(awaiting.isActive).isTrue()

        childScope.cancel()
        runCurrent()

        assertThat(awaiting.isCancelled).isTrue()
        session.close()
    }

    @Test
    fun defaultNanoTime_advancesWithTimeSource() = runTest {
        val session =
            HeadlessSession(
                parentContext = currentCoroutineContext(),
                name = "defaultNanoTime",
                timeSource = testScheduler.timeSource,
            )
        coroutineScope {
            session.start(this)
            session.awaitRunning()
            advanceTimeBy(25)
            var frameTimeNanos = -1L
            val composition = session.setContent {
                LaunchedEffect(Unit) { withFrameNanos { frameTimeNanos = it } }
            }

            assertThat(session.awaitQuiescence()).isEqualTo(QuiescenceResult.Quiescent)
            assertThat(frameTimeNanos).isEqualTo(25_000_000L)
            session.close()
            composition.dispose()
        }
    }

    @Test
    fun awaitQuiescence_unstableComposition_returnsUnstable() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            session.awaitRunning()
            var tick by mutableStateOf(0)
            val composition = session.setContent {
                // Mutates state on every recomposition without requesting frames, exhausting
                // maxIterations rather than maxFrames.
                val observed = tick
                LaunchedEffect(observed) { tick = observed + 1 }
            }

            val result = session.awaitQuiescence(maxIterations = 5)

            assertThat(result).isEqualTo(QuiescenceResult.Unstable)
            session.close()
            composition.dispose()
        }
    }

    @Test
    fun awaitQuiescence_effectYieldingAndLaunchingChildBeforeFrame_settlesCompletely() = runTest {
        val session = newSession()
        coroutineScope {
            session.start(this)
            session.awaitRunning()
            var state by mutableStateOf("initial")
            var composed = ""
            val composition = session.setContent {
                composed = state
                LaunchedEffect(Unit) {
                    yield()
                    launch {
                        yield()
                        withFrameNanos { state = "settled" }
                    }
                }
            }

            val result = session.awaitQuiescence()

            assertThat(result).isEqualTo(QuiescenceResult.Quiescent)
            assertThat(composed).isEqualTo("settled")
            session.close()
            composition.dispose()
        }
    }

    @Test
    fun awaitQuiescence_onRawThreadWithEmptyContext_fallsBackToDefaultDispatcherAndSettles() {
        val executor = Executors.newSingleThreadExecutor()
        val outcome = AtomicReference<Result<String>>()
        val done = CountDownLatch(1)
        try {
            executor.execute {
                val block: suspend () -> String = {
                    val session = HeadlessSession(EmptyCoroutineContext, name = "raw")
                    var value by mutableStateOf("init")
                    var seen = ""
                    val composition = Composition(NoopApplier, session.recomposer)
                    try {
                        coroutineScope {
                            session.start(this)
                            session.awaitRunning()
                            withContext(session.recomposerDispatcher) {
                                composition.setContent {
                                    seen = value
                                    LaunchedEffect(Unit) { withFrameNanos { value = "done" } }
                                }
                            }
                            check(session.awaitQuiescence() == QuiescenceResult.Quiescent)
                            session.close()
                        }
                        seen
                    } finally {
                        session.close()
                        composition.dispose()
                    }
                }
                block.startCoroutine(
                    object : Continuation<String> {
                        override val context = EmptyCoroutineContext

                        override fun resumeWith(result: Result<String>) {
                            outcome.set(result)
                            done.countDown()
                        }
                    }
                )
            }
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue()
            assertThat(outcome.get().getOrThrow()).isEqualTo("done")
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun awaitQuiescence_effectWriteClaimedByAnotherThreadsApply_waitsForDelivery() = runBlocking {
        val text = mutableStateOf("init")
        val claimed = CountDownLatch(1)
        val awaitingDelivery = CountDownLatch(1)
        val claimingThreadName = "HeadlessSessionTest-Claimer"
        val delayingObserver = Snapshot.registerApplyObserver { changed, _ ->
            if (text in changed && Thread.currentThread().name == claimingThreadName) {
                claimed.countDown()
                awaitingDelivery.await(5, TimeUnit.SECONDS)
            }
        }
        onCaptureAwaitingWriteDelivery = {
            if (claimed.count == 0L) awaitingDelivery.countDown()
        }
        try {
            withContext(Dispatchers.Default) {
                val session = HeadlessSession(currentCoroutineContext(), name = "claimedWrite")
                val composition = Composition(NoopApplier, session.recomposer)
                var seen = ""
                try {
                    coroutineScope {
                        session.start(this)
                        session.awaitRunning()
                        withContext(session.recomposerDispatcher) {
                            composition.setContent {
                                seen = text.value
                                LaunchedEffect(Unit) {
                                    withFrameNanos {}
                                    val snapshot = Snapshot.takeMutableSnapshot()
                                    text.value = "delivered"
                                    val claimer =
                                        Thread(
                                            {
                                                try {
                                                    snapshot.apply().check()
                                                } finally {
                                                    snapshot.dispose()
                                                }
                                            },
                                            claimingThreadName,
                                        )
                                    claimer.start()
                                    assertThat(claimed.await(5, TimeUnit.SECONDS)).isTrue()
                                }
                            }
                        }
                        assertThat(session.awaitQuiescence()).isEqualTo(QuiescenceResult.Quiescent)
                        assertThat(seen).isEqualTo("delivered")
                        session.close()
                    }
                } finally {
                    session.close()
                    composition.dispose()
                }
            }
        } finally {
            onCaptureAwaitingWriteDelivery = null
            delayingObserver.dispose()
        }
    }

    @Test
    fun pollUntil_withFastDispatcher_waitsForCrossThreadCompletionBeforeTimeout() = runTest {
        val done = AtomicBoolean(false)
        val started = CountDownLatch(1)
        val parked = CountDownLatch(1)
        val polls = AtomicInteger(0)
        val worker =
            Thread(
                    {
                        started.countDown()
                        parked.await(5, TimeUnit.SECONDS)
                        done.set(true)
                    },
                    "HeadlessSessionTest-PollUntilWorker",
                )
                .also { it.start() }
        try {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
            val completed =
                withContext(Dispatchers.Unconfined) {
                    pollUntil(canYield = true, timeout = 500.milliseconds) {
                        if (polls.incrementAndGet() >= APPLY_NOTIFICATION_SPIN_POLLS + 5) {
                            parked.countDown()
                        }
                        done.get()
                    }
                }
            assertThat(completed).isTrue()
        } finally {
            parked.countDown()
            worker.join(5_000)
        }
    }

    @Test
    fun pollUntil_whenConditionStaysFalse_returnsFalseAtTimeout() = runTest {
        val completed = pollUntil(canYield = true, timeout = 5.milliseconds) { false }
        assertThat(completed).isFalse()
    }

    @Test
    fun awaitQuiescence_inFlightSendApplyNotificationsOnAnotherThread_waitsWithinTimeout() =
        runTest {
            val text = mutableStateOf("init")
            val notificationInFlight = CountDownLatch(1)
            val awaitingNotifications = CountDownLatch(1)
            val notifyingThreadName = "HeadlessSessionTest-Notifier"
            var notifier: Thread? = null
            val delayingObserver = Snapshot.registerApplyObserver { changed, _ ->
                if (text in changed && Thread.currentThread().name == notifyingThreadName) {
                    notificationInFlight.countDown()
                    awaitingNotifications.await(5, TimeUnit.SECONDS)
                }
            }
            onAwaitingApplyObserverNotifications = {
                if (notificationInFlight.count == 0L) awaitingNotifications.countDown()
            }
            val session = newSession()
            try {
                coroutineScope {
                    session.start(this)
                    session.awaitRunning()
                    var seen = ""
                    val composition = session.setContent {
                        seen = text.value
                        LaunchedEffect(Unit) {
                            text.value = "delivered"
                            notifier =
                                Thread({ Snapshot.sendApplyNotifications() }, notifyingThreadName)
                                    .also { it.start() }
                            assertThat(notificationInFlight.await(5, TimeUnit.SECONDS)).isTrue()
                        }
                    }

                    val result = session.awaitQuiescence()

                    assertThat(result).isEqualTo(QuiescenceResult.Quiescent)
                    assertThat(seen).isEqualTo("delivered")
                    session.close()
                    composition.dispose()
                }
            } finally {
                onAwaitingApplyObserverNotifications = null
                awaitingNotifications.countDown()
                session.close()
                notifier?.join(5_000)
                delayingObserver.dispose()
            }
        }
}
