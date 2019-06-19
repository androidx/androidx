/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@InternalCoroutinesApi
@SmallTest
@RunWith(AndroidJUnit4::class)
class PausingDispatcherTest {
    // TODO update custom dispatchers with the new TestCoroutineContext once available
    // https://github.com/Kotlin/kotlinx.coroutines/pull/890
    private val taskTracker = TaskTracker()
    // track uncaught exceptions on the test scope
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        testError = testError ?: throwable
    }
    // did we hit any error in the test scope
    private var testError: Throwable? = null
    // the executor for the testing scope that uses a different thread pool
    private val testExecutor = TrackedExecutor(taskTracker, Executors.newFixedThreadPool(4))
    private val testingScope =
        CoroutineScope(testExecutor.asCoroutineDispatcher() + Job(null) + exceptionHandler)
    private val owner = FakeLifecycleOwner(Lifecycle.State.RESUMED)
    private val mainExecutor = TrackedExecutor(taskTracker, Executors.newSingleThreadExecutor())
    // tracks execution order
    private val expectations = Expectations()
    private lateinit var mainThread: Thread

    @ExperimentalCoroutinesApi
    @Before
    fun updateMainHandlerAndDispatcher() {
        Dispatchers.setMain(mainExecutor.asCoroutineDispatcher())
        runBlocking(Dispatchers.Main) {
            // extract the main thread to field for assertions
            mainThread = Thread.currentThread()
        }
    }

    @ExperimentalCoroutinesApi
    @After
    fun clearHandlerAndDispatcher() {
        waitTestingScopeChildren()
        assertThat(mainExecutor.shutdown(10, TimeUnit.SECONDS)).isTrue()
        assertThat(testExecutor.shutdown(10, TimeUnit.SECONDS)).isTrue()
        assertThat(taskTracker.awaitIdle(10, TimeUnit.SECONDS)).isTrue()
        Dispatchers.resetMain()
    }

    /**
     * Ensure nothing in the testing scope is left behind w/o assertions
     */
    private fun waitTestingScopeChildren() {
        runBlocking {
            val testJob = testingScope.coroutineContext[Job]!!
            do {
                val children = testJob.children.toList()
                assertThat(children.all {
                    withTimeoutOrNull(10_000) {
                        it.join()
                        true
                    } ?: false
                })
            } while (children.isNotEmpty())
            assertThat(testJob.isActive)
            assertThat(testError).isNull()
        }
    }

    @Test
    fun basic() {
        val result = runBlocking {
            owner.whenResumed {
                assertThread()
                3
            }
        }
        assertThat(result).isEqualTo(3)
    }

    @Test
    fun yieldTest() {
        runBlocking(Dispatchers.Main) {
            owner.whenResumed {
                expectations.expect(1)
                launch {
                    expectations.expect(3)
                    yield()
                    expectations.expect(5)
                }
                expectations.expect(2)
                launch {
                    expectations.expect(4)
                }
            }
            expectations.expectTotal(5)
        }
    }

    @Test
    fun runInsideMain() {
        val res = runBlocking(Dispatchers.Main) {
            owner.whenResumed {
                2
            }
        }
        assertThat(res).isEqualTo(2)
    }

    @Test
    fun moveToAnotherDispatcher() {
        val result = runBlocking {
            owner.whenResumed {
                assertThread()
                val innerResult = withContext(testingScope.coroutineContext) {
                    log("running inner")
                    "hello"
                }
                assertThread()
                log("received inner result $innerResult")
                innerResult + innerResult
            }
        }
        assertThat(result).isEqualTo("hellohello")
    }

    @Test
    fun cancel() {
        runBlocking {
            val job = testingScope.launch {
                owner.whenResumed {
                    try {
                        expectations.expect(1)
                        delay(5000)
                        expectations.expectUnreached()
                    } finally {
                        expectations.expect(2)
                    }
                }
            }
            drain()
            expectations.expectTotal(1)
            job.cancelAndJoin()
            expectations.expectTotal(2)
        }
    }

    @Test
    fun throwException_thenRunAnother() {
        runBlocking(testingScope.coroutineContext) {
            try {
                @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                owner.whenResumed {
                    assertThread()
                    expectations.expect(1)
                    throw IllegalArgumentException(" fail")
                }
                @Suppress("UNREACHABLE_CODE")
                expectations.expectUnreached()
            } catch (ignored: IllegalArgumentException) {
            }
            owner.whenResumed {
                expectations.expect(2)
            }
        }
        expectations.expectTotal(2)
    }

    @Test
    fun innerThrowException() {
        runBlocking {
            val job = testingScope.launch {
                val res = runCatching {
                    owner.whenResumed {
                        try {
                            expectations.expect(1)
                            @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                            withContext(testingScope.coroutineContext) {
                                throw IllegalStateException("i fail")
                            }
                            @Suppress("UNREACHABLE_CODE")
                            expectations.expectUnreached()
                        } finally {
                            expectations.expect(2)
                        }
                        @Suppress("UNREACHABLE_CODE")
                        expectations.expectUnreached()
                    }
                }
                assertThat(res.exceptionOrNull()).hasMessageThat().isEqualTo("i fail")
            }
            job.join()
            expectations.expectTotal(2)
        }
    }

    @Test
    fun pause_thenResume() {
        owner.pause()
        runBlocking {
            val job = testingScope.launch {
                owner.whenResumed {
                    expectations.expect(1)
                }
            }
            drain()
            expectations.expectTotal(0)
            owner.resume()
            job.join()
            expectations.expectTotal(1)
        }
    }

    @Test
    fun pause_thenFinish() {
        owner.pause()
        runBlocking {
            val job = testingScope.launch {
                owner.whenResumed {
                    try {
                        expectations.expectUnreached()
                    } finally {
                        expectations.expectUnreached()
                    }
                }
            }
            drain()
            expectations.expectTotal(0)
            owner.destroy()
            job.join()
            // never started so shouldn't run finally either
            expectations.expectTotal(0)
        }
    }

    @Test
    fun finishWhileDelayed() {
        runBlocking {
            val job = testingScope.launch {
                owner.whenResumed {
                    try {
                        expectations.expect(1)
                        delay(100000)
                        expectations.expectUnreached()
                    } finally {
                        expectations.expect(2)
                        assertThat(isActive).isFalse()
                    }
                }
            }
            drain()
            expectations.expectTotal(1)
            owner.destroy()
            job.join()
            expectations.expectTotal(2)
        }
    }

    @Test
    fun innerScopeFailure() {
        runBlocking {
            owner.whenResumed {
                val error = CompletableDeferred<Throwable>()
                val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                    error.complete(throwable)
                }
                launch(Job() + exceptionHandler) {
                    throw IllegalStateException("i fail")
                }
                val a2 = async {
                    expectations.expect(1)
                }
                assertThat(error.await()).hasMessageThat().contains("i fail")
                a2.await()
            }
            expectations.expectTotal(1)
        }
    }

    @Test
    fun alreadyFinished() {
        runBlocking {
            owner.destroy()
            launch {
                owner.whenResumed {
                    expectations.expectUnreached()
                }
            }.join()
            expectations.expectTotal(0)
        }
    }

    @Test
    fun catchFinishWhileDelayed() {
        runBlocking {
            val job = testingScope.launch {
                owner.whenResumed {
                    try {
                        expectations.expect(1)
                        delay(100000)
                        expectations.expectUnreached()
                    } catch (e: Exception) {
                        expectations.expect(2)
                        assertThat(isActive).isFalse()
                    } finally {
                        expectations.expect(3)
                    }
                    expectations.expect(4)
                }
            }
            drain()
            expectations.expectTotal(1)
            owner.destroy()
            job.join()
            expectations.expectTotal(4)
        }
    }

    @Test
    fun pauseThenContinue() {
        runBlocking {
            val job = testingScope.launch {
                owner.whenResumed {
                    expectations.expect(1)
                    withContext(testingScope.coroutineContext) {
                        owner.pause()
                    }
                    expectations.expect(2)
                }
                expectations.expect(3)
            }
            drain()
            expectations.expectTotal(1)
            assertThat(owner.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
            owner.resume()
            job.join()
            expectations.expectTotal(3)
        }
    }

    @Test
    fun parentJobCancelled() {
        runBlocking {
            val parent = testingScope.launch {
                owner.whenResumed {
                    try {
                        expectations.expect(1)
                        delay(5000)
                        expectations.expectUnreached()
                    } finally {
                        expectations.expect(2)
                    }
                }
            }
            drain()
            expectations.expectTotal(1)
            parent.cancelAndJoin()
            expectations.expectTotal(2)
        }
    }

    @Test
    fun innerJobCancelsParent() {
        try {
            @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
            runBlocking(testingScope.coroutineContext) {
                owner.whenResumed {
                    throw IllegalStateException("i fail")
                }
            }
            @Suppress("UNREACHABLE_CODE")
            expectations.expectUnreached()
        } catch (ex: IllegalStateException) {
            assertThat(ex).hasMessageThat().isEqualTo("i fail")
        }
    }

    @Test
    fun lifecycleInsideLifecycle() {
        runBlocking {
            owner.whenResumed {
                assertThread()
                expectations.expect(1)
                owner.whenResumed {
                    assertThread()
                    expectations.expect(2)
                }
            }
        }
        expectations.expectTotal(2)
    }

    @Test
    fun lifecycleInsideLifecycle_innerFails() {
        runBlocking {
            val res = runCatching {
                owner.whenResumed {
                    try {
                        assertThread()
                        expectations.expect(1)
                        owner.whenResumed {
                            assertThread()
                            expectations.expect(2)
                            try {
                                @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
                                withContext(testingScope.coroutineContext) {
                                    throw IllegalStateException("i fail")
                                }
                                @Suppress("UNREACHABLE_CODE")
                                expectations.expectUnreached()
                            } finally {
                                expectations.expect(3)
                            }
                        }
                        expectations.expectUnreached()
                    } finally {
                        expectations.expect(4)
                    }
                }
            }
            assertThat(res.exceptionOrNull()).hasMessageThat().matches("i fail")
        }
        expectations.expectTotal(4)
    }

    @Test
    fun cancelInnerCoroutine() {
        runBlocking {
            val job = launch {
                owner.whenResumed {
                    withContext(testingScope.coroutineContext) {
                        delay(200_000)
                        expectations.expectUnreached()
                    }
                    expectations.expectUnreached()
                }
            }
            job.cancelAndJoin()
            expectations.expectTotal(0)
        }
    }

    @Test
    fun launchWhenCreated() {
        val owner = FakeLifecycleOwner()
        val latchStart = CountDownLatch(1)
        val job = owner.lifecycleScope.launchWhenCreated {
            latchStart.countDown()
            delay(100000)
            expectations.expectUnreached()
        }
        assertThat(latchStart.count).isEqualTo(1)
        owner.create()
        assertThat(latchStart.await(10, TimeUnit.SECONDS)).isTrue()
        owner.destroy()
        assertJobCancelled(job)
        drain()
    }

    @Test
    fun launchWhenStarted() {
        val owner = FakeLifecycleOwner(Lifecycle.State.CREATED)
        val latchStart = CountDownLatch(1)
        val job = owner.lifecycleScope.launchWhenStarted {
            latchStart.countDown()
            delay(100000)
            expectations.expectUnreached()
        }
        drain()
        assertThat(latchStart.count).isEqualTo(1)
        owner.start()
        assertThat(latchStart.await(10, TimeUnit.SECONDS)).isTrue()
        owner.destroy()
        assertJobCancelled(job)
        drain()
    }

    @Test
    fun launchWhenResumed() {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)
        val latchStart = CountDownLatch(1)
        val job = owner.lifecycleScope.launchWhenResumed {
            latchStart.countDown()
            delay(100000)
            expectations.expectUnreached()
        }
        drain()
        assertThat(latchStart.count).isEqualTo(1)
        owner.resume()
        assertThat(latchStart.await(10, TimeUnit.SECONDS)).isTrue()
        owner.destroy()
        assertJobCancelled(job)
        drain()
    }

    private fun assertJobCancelled(job: Job) {
        runBlocking {
            withTimeout(1000) {
                job.join()
            }
            assertThat(job.isCancelled).isTrue()
        }
    }

    private fun assertThread() {
        log("asserting looper")
        assertThat(Thread.currentThread()).isSameInstanceAs(mainThread)
    }

    private fun log(msg: Any?) {
        Log.d("TEST-RUN", "[${Thread.currentThread().name}] $msg")
    }

    private fun drain() {
        assertThat(taskTracker.awaitIdle(10, TimeUnit.SECONDS)).isTrue()
    }
}