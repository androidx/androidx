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

package androidx.compose.ui.inspection.test

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.IntState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.inspection.test.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.test.rules.sendCommand
import androidx.compose.ui.inspection.test.testdata.TestActivity
import androidx.compose.ui.inspection.test.util.GetComposablesCommand
import androidx.compose.ui.inspection.test.util.GetRecompositionStateReadCommand
import androidx.compose.ui.inspection.test.util.GetUpdateSettingsCommand
import androidx.compose.ui.inspection.test.util.UnitApplier
import androidx.compose.ui.inspection.test.util.filter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import org.junit.After
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TIMEOUT_MS = 5_000L
private const val INSPECTOR_THREAD_NAME = "Test Inspector Handler Thread"
private const val BACKGROUND_THREAD_NAME = "Background Composition Thread"

/**
 * Regression tests for b/581014660: observing state reads must not deadlock with a composition that
 * is recomposing.
 * - Stopping state read observation (or disposing the inspector) from the inspector thread while
 *   the main thread is paused inside a recomposition (holding the composition lock). When the main
 *   thread resumes, it reads a state, starts a restartable group and creates a sub-composition,
 *   which all call into the inspector.
 * - Starting state read observation while a composition on a background thread is paused inside a
 *   recomposition. The Recomposer reports the existing compositions to the inspector while holding
 *   the Recomposer lock. When the background thread resumes, it creates a sub-composition, which
 *   takes the Recomposer lock while the composition lock is held.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StateReadHandlerDeadlockTest {
    @get:Rule val rule = ComposeInspectionRule(TestActivity::class)

    private val content = BlockingContent()

    @After
    fun releaseMainThread() {
        content.release.countDown()
    }

    @Test
    fun stopObservingStateReadsDuringRecomposition() {
        pauseMainThreadInRecomposition()
        val task = BackgroundTask {
            runBlocking { rule.inspectorTester.sendCommand(GetUpdateSettingsCommand()) }
        }
        resumeMainThreadWhenInspectorThreadIsBlocked(task.done)
        task.done.awaitOrFailWithDeadlock("UpdateSettingsCommand(NONE) did not complete")
        task.rethrowFailure()
        assertMainThreadResponsive()
    }

    @Test
    fun disposeDuringRecomposition() {
        pauseMainThreadInRecomposition()
        val task = BackgroundTask { rule.inspectorTester.dispose() }
        resumeMainThreadWhenInspectorThreadIsBlocked(task.done)
        task.done.awaitOrFailWithDeadlock("Inspector dispose did not complete")
        task.rethrowFailure()
        assertMainThreadResponsive()
    }

    @Test
    fun startObservingStateReadsWhileBackgroundCompositionIsRecomposing() {
        BackgroundComposition().use { background ->
            background.pauseInRecomposition()
            runBlocking {
                rule.inspectorTester.sendCommand(
                    GetUpdateSettingsCommand(
                        includeRecomposeCounts = true,
                        stateReadKind = StateReadSettings.Kind.ALL,
                    )
                )
            }
            // The main thread observes the recomposer, and then waits for the lock of the paused
            // composition to set the composition observer:
            waitUntilMainThreadIsBlockedInComposition()
            background.release.countDown()
            background.recomposed.awaitOrFailWithDeadlock("The background composition is stuck")
            assertMainThreadResponsive()
        }
    }

    /**
     * Enables state read observation, waits until the composition observer is installed, and then
     * starts a recomposition that pauses the main thread inside the composition until
     * [BlockingContent.release] is counted down.
     */
    private fun pauseMainThreadInRecomposition() {
        rule.show { BlockingComposable(content) }
        runBlocking {
            rule.inspectorTester.sendCommand(
                GetUpdateSettingsCommand(
                    includeRecomposeCounts = true,
                    stateReadKind = StateReadSettings.Kind.ALL,
                )
            )
            val anchorHash =
                rule.inspectorTester
                    .sendCommand(GetComposablesCommand(rule.rootId, skipSystemComposables = false))
                    .getComposablesResponse
                    .filter("BlockingComposable")
                    .single()
                    .anchorHash

            // The composition observer is installed asynchronously on the main thread.
            // Recompose until a state read is reported, which proves the observer is installed.
            val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
            while (true) {
                recompose()
                val reads =
                    rule.inspectorTester
                        .sendCommand(
                            GetRecompositionStateReadCommand(
                                anchorHash,
                                recompositionNumberStart = 0,
                                recompositionNumberEnd = Int.MAX_VALUE,
                                includeExtra = true,
                            )
                        )
                        .getRecompositionStateReadResponse
                if (reads.readCount > 0) break
                if (SystemClock.uptimeMillis() > deadline) {
                    fail("No state reads were observed for BlockingComposable")
                }
            }
        }
        content.blockAt = content.trigger.intValue + 1
        content.trigger.intValue = content.blockAt
        if (!content.entered.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            fail("The main thread did not reach the paused recomposition")
        }
    }

    /** Triggers a recomposition of [BlockingComposable] and waits until it has happened. */
    private fun recompose() {
        content.composed.clear()
        val value = content.trigger.intValue + 1
        content.trigger.intValue = value
        while (true) {
            val composed =
                content.composed.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    ?: throw AssertionError("BlockingComposable did not recompose")
            if (composed == value) return
        }
    }

    /**
     * Resumes the paused main thread once the inspector thread is blocked waiting for the
     * composition lock held by the paused main thread, or once the inspector operation is [done]
     * (i.e. it didn't need the composition lock).
     */
    private fun resumeMainThreadWhenInspectorThreadIsBlocked(done: CountDownLatch) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (!done.await(10, TimeUnit.MILLISECONDS)) {
            if (inspectorThreads().any { it.isBlockedInComposition() }) break
            if (SystemClock.uptimeMillis() > deadline) {
                fail(
                    "The inspector thread neither completed nor blocked while the main thread " +
                        "was paused in a recomposition:\n${dumpThreads()}"
                )
            }
        }
        content.release.countDown()
    }

    private fun waitUntilMainThreadIsBlockedInComposition() {
        val mainThread = Looper.getMainLooper().thread
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (!mainThread.isBlockedInComposition()) {
            if (SystemClock.uptimeMillis() > deadline) {
                fail("The main thread did not try to observe the composition:\n${dumpThreads()}")
            }
            Thread.yield()
        }
    }

    private fun assertMainThreadResponsive() {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post { latch.countDown() }
        latch.awaitOrFailWithDeadlock("The main thread is not responsive")
    }

    /**
     * Fails the test if the latch is not counted down in time, which means that the main thread is
     * deadlocked. A deadlocked main thread can't be recovered, and the teardown of this test and
     * all following tests would hang. Throw the failure from a separate thread instead: the test
     * runner reports it as the failure of this test and then terminates the process.
     */
    private fun CountDownLatch.awaitOrFailWithDeadlock(message: String) {
        if (await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) return
        val error = AssertionError("$message. Deadlock detected:\n${dumpThreads()}")
        thread(name = "StateReadHandlerDeadlockTest failure") { throw error }.join(TIMEOUT_MS)
        throw error
    }

    private fun Thread.isBlockedInComposition(): Boolean =
        state == Thread.State.BLOCKED &&
            stackTrace.any { it.className.startsWith("androidx.compose.runtime.CompositionImpl") }

    private fun inspectorThreads(): List<Thread> =
        Thread.getAllStackTraces().keys.filter { it.name == INSPECTOR_THREAD_NAME }

    private fun dumpThreads(): String =
        Thread.getAllStackTraces()
            .filter { (thread, _) ->
                thread == Looper.getMainLooper().thread ||
                    thread.name == INSPECTOR_THREAD_NAME ||
                    thread.name == BACKGROUND_THREAD_NAME
            }
            .entries
            .joinToString("\n") { (thread, stack) ->
                "\"${thread.name}\" ${thread.state}\n" +
                    stack.take(15).joinToString("\n") { "    at $it" }
            }
}

/** Runs [block] on a new thread. [done] is counted down when [block] completes. */
private class BackgroundTask(block: () -> Unit) {
    val done = CountDownLatch(1)
    @Volatile private var failure: Throwable? = null

    init {
        thread(name = "StateReadHandlerDeadlockTest") {
            try {
                block()
            } catch (e: Throwable) {
                failure = e
            } finally {
                done.countDown()
            }
        }
    }

    fun rethrowFailure() {
        failure?.let { throw it }
    }
}

private class BlockingContent {
    val trigger = mutableIntStateOf(0)
    val other = mutableIntStateOf(0)
    val composed = LinkedBlockingQueue<Int>()
    @Volatile var blockAt = -1
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
}

@Composable
private fun BlockingComposable(content: BlockingContent) {
    val value = content.trigger.intValue
    val paused = value == content.blockAt
    if (paused) {
        content.entered.countDown()
        // Never block forever, even if the test fails before releasing the main thread.
        content.release.await(4 * TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }
    // After the pause: read a state and start a restartable group. Both call into the inspector.
    Reader(content.other.intValue)
    if (paused) {
        // Create a new sub-composition, which registers it with the inspector while holding the
        // Recomposer lock.
        SubComposition(value)
    }
    content.composed.offer(value)
}

@Composable
private fun SubComposition(value: Int) {
    key(value) {
        val context = rememberCompositionContext()
        val subComposition = remember { Composition(UnitApplier(), context) }
        // This composes the sub-composition now, which takes the Recomposer lock:
        subComposition.setContent { SubContent(value) }
        DisposableEffect(subComposition) { onDispose { subComposition.dispose() } }
    }
}

@Composable
private fun Reader(value: Int) {
    BasicText("$value")
}

/**
 * A composition on a background thread, which can be paused in a recomposition with
 * [pauseInRecomposition] until [release] is counted down.
 */
private class BackgroundComposition : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor { Thread(it, BACKGROUND_THREAD_NAME) }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val clock = BroadcastFrameClock()
    private val job = Job()
    private val scope = CoroutineScope(dispatcher + clock + job)
    private val recomposer = Recomposer(scope.coroutineContext)
    private val composition = Composition(UnitApplier(), recomposer)
    private val count = mutableIntStateOf(0)
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    val recomposed = CountDownLatch(1)

    init {
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        runBlocking {
            withTimeout(TIMEOUT_MS) {
                recomposer.currentState.first { it == Recomposer.State.Idle }
            }
            withContext(dispatcher) {
                composition.setContent { Pausing(count, this@BackgroundComposition) }
            }
        }
    }

    /** Start a recomposition, and wait until it is paused while holding the composition lock. */
    fun pauseInRecomposition() {
        Snapshot.withMutableSnapshot { count.intValue = 1 }
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (!entered.await(5, TimeUnit.MILLISECONDS)) {
            // The recomposition happens on the thread that sends the frame:
            executor.execute { clock.sendFrame(System.nanoTime()) }
            if (SystemClock.uptimeMillis() > deadline) {
                fail("The background composition did not recompose")
            }
        }
    }

    override fun close() {
        release.countDown()
        // Do not wait for the composition thread on the test thread: it may be deadlocked.
        thread {
                executor.execute {
                    composition.dispose()
                    recomposer.cancel()
                }
                job.cancel()
                scope.cancel()
                executor.shutdown()
                executor.awaitTermination(1, TimeUnit.SECONDS)
            }
            .join(2 * 1000L)
    }
}

/** Pauses in the recomposition where [count] is 1, and then creates a new sub-composition. */
@Composable
private fun Pausing(count: IntState, background: BackgroundComposition) {
    val value = count.intValue
    if (value == 1) {
        background.entered.countDown()
        // Never block forever, even if the test fails before releasing the thread.
        background.release.await(4 * TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }
    SubComposition(value)
    SideEffect {
        if (value == 1) {
            background.recomposed.countDown()
        }
    }
}

@Composable private fun SubContent(@Suppress("UNUSED_PARAMETER") value: Int) {}
