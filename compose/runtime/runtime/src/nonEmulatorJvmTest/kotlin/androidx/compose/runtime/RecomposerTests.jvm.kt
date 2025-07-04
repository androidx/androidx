/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.compose.runtime.mock.TestMonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

class RecomposerTestsJvm {

    @ExperimentalCoroutinesApi
    private fun runTestUnconfined(testBody: suspend TestScope.() -> Unit) {
        runTest(UnconfinedTestDispatcher(), testBody = testBody)
    }

    @Test
    @OptIn(ExperimentalComposeApi::class, ExperimentalCoroutinesApi::class)
    fun concurrentRecompositionOffMainThread() = runTestUnconfined {
        val dispatcher = testScheduler
        withContext(dispatcher) {
            val clock = TestMonotonicFrameClock(this)
            withContext(clock) {
                val recomposer = Recomposer(coroutineContext)
                launch { recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default) }

                val composition = Composition(UnitApplier(), recomposer)
                val threadLog = Channel<Thread>(Channel.BUFFERED)
                lateinit var recomposeScope: RecomposeScope
                composition.setContent {
                    threadLog.trySend(Thread.currentThread())
                    val scope = currentRecomposeScope
                    SideEffect { recomposeScope = scope }
                }

                val firstCompositionThread = threadLog.receive()

                recomposeScope.invalidate()
                dispatcher.advanceUntilIdle()

                val secondCompositionThread = threadLog.receive()
                assertNotEquals(firstCompositionThread, secondCompositionThread)

                recomposer.close()
                dispatcher.advanceUntilIdle()
            }
        }
    }

    @Test
    @OptIn(ExperimentalComposeApi::class, ExperimentalCoroutinesApi::class)
    fun concurrentRecompositionInvalidationDuringComposition() = runTestUnconfined {
        val dispatcher = testScheduler
        val clock = AutoTestFrameClock()
        withContext(dispatcher + clock) {
            val recomposer = Recomposer(coroutineContext)
            launch { recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default) }

            val composition = Composition(UnitApplier(), recomposer)
            var longRecomposition by mutableStateOf(false)
            val longRecompositionLatch = CountDownLatch(1)
            val applyCount = AtomicInteger(0)
            val recomposeLatch = CountDownLatch(2)
            composition.setContent {
                recomposeLatch.countDown()
                if (longRecomposition) {
                    longRecompositionLatch.await()
                }
                SideEffect { applyCount.incrementAndGet() }
            }

            assertEquals(1, applyCount.get(), "applyCount after initial composition")

            Snapshot.withMutableSnapshot { longRecomposition = true }

            assertTrue(recomposeLatch.await(5, TimeUnit.SECONDS), "recomposeLatch await timed out")
            assertEquals(1, applyCount.get(), "applyCount after starting long recomposition")

            longRecompositionLatch.countDown()
            recomposer.awaitIdle()

            assertEquals(2, applyCount.get(), "applyCount after long recomposition")

            recomposer.close()
        }
    }

    @Test
    @OptIn(ExperimentalComposeApi::class)
    fun concurrentRecompositionOnCompositionSpecificContext() =
        runBlocking(AutoTestFrameClock()) {
            val recomposer = Recomposer(coroutineContext)
            launch { recomposer.runRecomposeConcurrentlyAndApplyChanges(Dispatchers.Default) }

            @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
            newSingleThreadContext("specialThreadPool").use { pool ->
                val composition = Composition(UnitApplier(), recomposer, pool)
                var recomposition by mutableStateOf(false)
                val recompositionThread = Channel<Thread>(1)
                composition.setContent {
                    if (recomposition) {
                        recompositionThread.trySend(Thread.currentThread())
                    }
                }

                Snapshot.withMutableSnapshot { recomposition = true }

                assertTrue(
                    withTimeoutOrNull(3_000) { recompositionThread.receive() }
                        ?.name
                        ?.contains("specialThreadPool") == true,
                    "recomposition did not occur on expected thread",
                )

                recomposer.close()
            }
        }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun recompositionOnConcurrentSnapshotInvalidation() =
        runBlocking(AutoTestFrameClock() + Dispatchers.Default) {
            if (ComposeRuntimeFlags.isMovableContentUsageTrackingEnabled) {
                // Late changes is not used the same way as this test expects when this flag
                // is enabled (the applier is only called once, not twice) so this test is skipped.
                return@runBlocking
            }
            // The basic idea of this test is to reconstruct the exact conditions of the race
            // by inducing an artificial delay at the right moment so that the otherwise elusive
            // problem reproduces reliably
            // (that is, as long as the implementation details we rely upon stay the same).
            //
            // We use a single recomposer with 2 compositions:
            //   - the main one observes the done state and signals by completing `doneJob`
            //   - an auxiliary composition with a custom applier that blocks at certain moment
            //     during the initial composition performed on a separate thread
            //
            // There's 3 coroutines at play:
            //
            //   1. The one performing recomposition of both the compositions.
            //      We make it block briefly inside recordComposerModifications() while trying to
            //      acquire `CompositionImpl.lock` (locked by the coroutine #2)
            //
            //   2. Another one launched separately only to call `Composition.setContent()` for the
            //      auxiliary composition so that it blocks in `applyLateChanges()` while holding
            //      `CompositionImpl.lock`.
            //
            //   3. A coroutine that orchestrates everything by modifying snapshot state objects,
            //      so that the recomposer coroutine (#1) calls the problematic code.
            //      It calls `Snapshot.sendApplyNotifications()` at the right moment while
            //      the recomposer coroutine is blocked; these notifications are lost as soon as
            //      the test latch is released and the coroutines #1 and #2 proceed as usual.

            val recomposer = Recomposer(coroutineContext)
            launch { recomposer.runRecomposeAndApplyChanges() }

            val doneState = mutableStateOf(false)
            val doneJob = Job(coroutineContext.job)

            // the main composition
            Composition(UnitApplier(), recomposer).setContent {
                val isDone = doneState.value
                SideEffect {
                    if (isDone) {
                        doneJob.complete()
                    }
                }
            }

            val starter = CountDownLatch(1)
            val resumer = CountDownLatch(1)

            val applier =
                object : Applier<Unit> by UnitApplier() {
                    // Skip when called for the first time - from applyChanges();
                    // block when called right after that - from applyLateChanges().
                    //
                    // This is the most fragile part. It relies on the fact that applyChanges()
                    // calls drainPendingModificationsLocked(), which sets
                    // `CompositionImpl.pendingModifications` to null, making the next call to
                    // recordModificationsOf() - called from recordComposerModifications() -
                    // attempt to acquire the lock in order to call
                    // drainPendingModificationsLocked() again.
                    var countDown = 1

                    // called while holding `CompositionImpl.lock`
                    override fun onBeginChanges() {
                        if (countDown == 0) {
                            starter.countDown()
                            resumer.await()
                        }
                        countDown--
                    }
                }

            val auxState = mutableStateOf("foo")
            launch {
                // wrap with `runInterruptible` so that `resumer.await()` inside `onBeginChanges()`
                // doesn't block indefinitely if something goes wrong with the test setup and the
                // test coroutine gets cancelled with the timeout
                runInterruptible {
                    // the aux composition
                    Composition(applier, recomposer).setContent {
                        auxState.value
                        // make `lateChanges` non-empty so that the applier is called again

                        // NOTE: This is not true when isMovableContentUsageTrackingEnabled is
                        // enabled so this test is not run in this case
                        movableContentOf {}()
                    }
                }
            }

            val orchestratingJob = launch {
                try {
                    runInterruptible { starter.await() }
                    // wake up the recomposer to process an unrelated invalidation
                    auxState.value = "bar"
                    Snapshot.sendApplyNotifications()
                    // spin until the recomposition loop calls recordComposerModifications(),
                    // and the latter reads and resets the `snapshotInvalidations`
                    while (recomposer.hasPendingWork) {
                        yield()
                    }
                    // recordComposerModifications() now proceeds onto calling
                    // `composition.recordModificationsOf(changes)`, one of which is going to block
                    // until `resumer` is released in the 'finally' block below

                    // the following is missed due to a race condition
                    doneState.value = true
                    Snapshot.sendApplyNotifications()
                    assertTrue(
                        recomposer.hasPendingWork,
                        "Expected Recomposer.hasPendingWork after Snapshot.sendApplyNotifications()",
                    )
                    // the state recorded into `snapshotInvalidations` is going to be lost
                    // as soon as recordComposerModifications() resumes
                    // and mistakenly resets `snapshotInvalidations` for the second time
                } finally {
                    resumer.countDown()
                }
            }

            assertNotNull(
                withTimeoutOrNull(3.seconds) { orchestratingJob.join() },
                "timed out waiting for orchestratingJob; doneState.value = ${doneState.value}",
            )
            assertTrue(doneState.value, "Test setup failed")
            assertNotNull(
                withTimeoutOrNull(3.seconds) { doneJob.join() },
                "Missed recomposition after setting `done` state",
            )

            coroutineContext.cancelChildren()
        }

    @Test
    fun recompositionOnConcurrentSnapshotInvalidationStressTest() =
        // This is a simplified version of test recompositionOnConcurrentSnapshotInvalidation()
        // using just brute force. It's not a 100% reproducer of the regression, but it's good
        // enough. More importantly, it reproduces the issue in a more future-proof way,
        // without tinkering with a custom applier or relying on implementation details
        // like how movable content or late changes work.
        runBlocking(AutoTestFrameClock() + Dispatchers.Default) {
            val recomposer = Recomposer(coroutineContext)
            launch { recomposer.runRecomposeAndApplyChanges() }

            val n = 150 // enough to get reproducible runs in 95-th percentile (YMMV)
            val countState = mutableStateOf(0)
            val channel = Channel<Int>(Channel.CONFLATED).apply { trySend(element = 0) }

            Composition(UnitApplier(), recomposer).setContent {
                val count = countState.value
                SideEffect {
                    if (count <= n) {
                        channel.trySend(element = count)
                    } else channel.close()
                }
            }

            val auxState = mutableStateOf(0)
            Composition(UnitApplier(), recomposer).setContent { auxState.value }

            try {
                @OptIn(FlowPreview::class) // for .timeout()
                channel.consumeAsFlow().timeout(3.seconds).collect { iteration ->
                    auxState.value = iteration
                    Snapshot.sendApplyNotifications()
                    while (recomposer.hasPendingWork) {
                        yield()
                    }
                    // the following might be missed due to a race condition
                    countState.value = iteration + 1
                    Snapshot.sendApplyNotifications()
                }
            } catch (_: TimeoutCancellationException) {
                fail("Missed recomposition on iteration #${countState.value}")
            }
            coroutineContext.cancelChildren()
        }

    @Test
    fun recomposerCancelReportsShuttingDownImmediately() =
        runBlocking(AutoTestFrameClock()) {
            val recomposer = Recomposer(coroutineContext)
            launch(start = CoroutineStart.UNDISPATCHED) { recomposer.runRecomposeAndApplyChanges() }

            // Create a composition with a LaunchedEffect that will need to be resumed for
            // cancellation
            // before the recomposer can fully join.
            Composition(UnitApplier(), recomposer).setContent {
                LaunchedEffect(Unit) { awaitCancellation() }
            }

            recomposer.cancel()
            // runBlocking will not dispatch resumed continuations for cancellation yet;
            // read the current state immediately.
            val state = recomposer.currentState.value
            assertTrue(
                state <= Recomposer.State.ShuttingDown,
                "recomposer state $state but expected <= ShuttingDown",
            )
        }

    @Test
    fun disposedInvalidatedCompositionDoesNotLeak(): Unit = runBlocking {
        val recomposer = Recomposer(coroutineContext)

        // Sent to when a frame is requested by recomposer
        val frameRequestCh = Channel<Unit>(Channel.CONFLATED)

        // Run recompositions with a clock that will never produce a frame, thereby leaving
        // invalidations unhandled. Launch undispatched to get things moving before we proceed.
        launch(
            BroadcastFrameClock { frameRequestCh.trySend(Unit) },
            start = CoroutineStart.UNDISPATCHED,
        ) {
            recomposer.runRecomposeAndApplyChanges()
        }

        // Used to invalidate the composition below
        var state by mutableStateOf(0)

        // Create the composition to test in a function rather than directly, otherwise
        // we end up with a hard reference from the stack sticking around preventing gc
        fun createWeakComposition() =
            WeakReference(
                Composition(UnitApplier(), recomposer).apply {
                    setContent {
                        // This state read will invalidate the composition
                        @Suppress("UNUSED_VARIABLE") val readme = state
                    }
                }
            )

        // Hold only a weak reference to this created composition for the test
        val weakRef = createWeakComposition()

        // Ensure the recomposer is idle and ready to receive invalidations before we commit
        // a snapshot that includes one
        recomposer.currentState.first { it == Recomposer.State.Idle }

        // Invalidate the composition
        Snapshot.withMutableSnapshot { state++ }

        withTimeoutOrNull(1000) { frameRequestCh.receive() }
            ?: fail("never requested a frame from recomposer")

        // Bug 209497244 tracked the Recomposer keeping this composition
        // in an invalidation list after disposal; confirm below that this becomes unreachable
        weakRef.get()?.dispose() ?: fail("composition prematurely collected")

        Runtime.getRuntime().gc()

        assertNull(weakRef.get(), "composition was not collected after disposal")

        recomposer.cancel()
    }
}

private class AutoTestFrameClock : MonotonicFrameClock {
    private val time = AtomicLong(0)

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        return onFrame(time.getAndAdd(16_000_000))
    }
}
