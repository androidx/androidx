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

package androidx.compose.ui

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionMutexTest {

    private val mutex = SessionMutex<TestSession>()

    @Test
    fun currentSessionInitiallyHasNoValue() {
        assertThat(mutex.currentSession).isNull()
    }

    @Test
    fun currentSessionReturnsWhileSessionRunning() = runTest {
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = { TestSession(it, "hello") },
                session = { awaitCancellation() }
            )
        }

        assertThat(mutex.currentSession).isNotNull()
        assertThat(mutex.currentSession!!.value).isEqualTo("hello")
    }

    @Test
    fun currentSessionReturnsNewSessionAfterInterruption() = runTest {
        launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = { TestSession(it, "hello") },
                session = { awaitCancellation() }
            )
        }
        assertThat(mutex.currentSession).isNotNull()
        assertThat(mutex.currentSession!!.value).isEqualTo("hello")
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = { TestSession(it, "world") },
                session = { awaitCancellation() }
            )
        }

        assertThat(mutex.currentSession).isNotNull()
        assertThat(mutex.currentSession!!.value).isEqualTo("world")
    }

    @Test
    fun currentSessionIsClearedAfterSessionReturns() = runTest {
        val finalizer = Job()
        launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = { TestSession(it, "hello") },
                session = { finalizer.join() }
            )
        }
        assertThat(mutex.currentSession).isNotNull()
        assertThat(mutex.currentSession!!.value).isEqualTo("hello")

        finalizer.complete()
        advanceUntilIdle()

        assertThat(mutex.currentSession).isNull()
    }

    @Test
    fun currentSessionIsClearedAfterSessionFails() = runTest {
        val finalizer = Job()
        launch(start = CoroutineStart.UNDISPATCHED) {
            assertFailsWith<TestException> {
                mutex.withSessionCancellingPrevious(
                    sessionInitializer = { TestSession(it, "hello") },
                    session = {
                        finalizer.join()
                        throw TestException()
                    }
                )
            }
        }
        assertThat(mutex.currentSession).isNotNull()
        assertThat(mutex.currentSession!!.value).isEqualTo("hello")

        finalizer.complete()
        advanceUntilIdle()

        assertThat(mutex.currentSession).isNull()
    }

    @Test
    fun currentSessionIsClearedAfterSessionIsCancelled() = runTest {
        val sessionJob = launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = { TestSession(it, "hello") },
                session = { awaitCancellation() }
            )
        }
        assertThat(mutex.currentSession).isNotNull()
        assertThat(mutex.currentSession!!.value).isEqualTo("hello")

        sessionJob.cancel()
        advanceUntilIdle()

        assertThat(mutex.currentSession).isNull()
    }

    @Test
    fun exceptionFromInitializer() = runTest {
        val exception = assertFails {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = { throw TestException() },
                session = { fail() }
            )
        }

        assertThat(exception).isInstanceOf(TestException::class.java)
    }

    @Test
    fun exceptionFromSession() = runTest {
        val exception = assertFails {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = ::TestSession,
                session = { throw TestException() }
            )
        }

        assertThat(exception).isInstanceOf(TestException::class.java)
    }

    @Test
    fun sessionReturnValue() = runTest {
        val result = mutex.withSessionCancellingPrevious(
            sessionInitializer = ::TestSession,
            session = { "hello" }
        )
        assertThat(result).isEqualTo("hello")
    }

    @Test
    fun sessionInitializerValueIsPassedToSession() = runTest {
        mutex.withSessionCancellingPrevious(
            sessionInitializer = { TestSession(it, "hello") },
            session = {
                assertThat(it.value).isEqualTo("hello")
            }
        )
    }

    @Test
    fun sessionCoroutineScopeIsPassedToInitializer() = runTest {
        val outerJob = coroutineContext.job
        withContext(CoroutineName("test name")) {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = {
                    assertThat(it.coroutineContext[CoroutineName]?.name).isEqualTo("test name")
                    assertThat(it.coroutineContext.job.isChildOf(outerJob)).isTrue()
                    TestSession(it)
                },
                session = {}
            )
        }
    }

    @Test
    fun suspendsUntilReturn() = runTest {
        val producer = CompletableDeferred<String>()
        val result = async {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = ::TestSession,
                session = { producer.await() }
            )
        }
        advanceUntilIdle()

        assertThat(result.isCompleted).isFalse()
        producer.complete("hello")

        assertThat(result.await()).isEqualTo("hello")
    }

    @Test
    fun newSessionInterruptsPrevious() = runTest {
        val firstSessionJob = launch {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = ::TestSession,
                session = { awaitCancellation() }
            )
        }
        advanceUntilIdle()
        assertThat(firstSessionJob.isCompleted).isFalse()

        mutex.withSessionCancellingPrevious(
            sessionInitializer = ::TestSession,
            session = {}
        )

        assertThat(firstSessionJob.isCancelled).isTrue()
    }

    @Test
    fun newSessionInterruptsPreviousWithoutSuspending() = runTest {
        var expected = 0
        fun expect(value: Int) {
            assertThat(expected).isEqualTo(value)
            expected++
        }

        launch {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = ::TestSession,
                session = {
                    expect(0)
                    suspendCancellableCoroutine<Nothing> { continuation ->
                        // This is ran synchronously by whoever calls cancel.
                        continuation.invokeOnCancellation {
                            expect(3)
                        }
                    }
                }
            )
        }
        advanceUntilIdle()
        launch(start = CoroutineStart.UNDISPATCHED) {
            expect(1)
            mutex.withSessionCancellingPrevious(
                sessionInitializer = {
                    expect(2)
                    TestSession(it)
                },
                session = {
                    expect(5)
                }
            )
        }

        expect(4)
        runCurrent()
        expect(6)
    }

    @Test
    fun newSessionWaitsForOldSessionToFinish() = runTest {
        var expected = 0
        fun expect(value: Int) {
            assertThat(expected).isEqualTo(value)
            expected++
        }

        launch {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = ::TestSession,
                session = {
                    expect(0)
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) {
                            delay(1000)
                            expect(2)
                        }
                    }
                }
            )
        }
        advanceUntilIdle()
        mutex.withSessionCancellingPrevious(
            sessionInitializer = {
                expect(1)
                TestSession(it)
            },
            session = {
                expect(3)
            }
        )

        expect(4)
    }

    @Test
    fun newSessionWaitsForOldSessionToFinishWhenSuspended() = runTest {
        val finalizer = Job()
        var secondSessionInitialized = false
        var secondSessionStarted = false

        val firstSessionJob = launch {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = ::TestSession,
                session = {
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) {
                            finalizer.join()
                        }
                    }
                }
            )
        }
        advanceUntilIdle()

        val secondSessionJob = launch {
            mutex.withSessionCancellingPrevious(
                sessionInitializer = {
                    secondSessionInitialized = true
                    TestSession(it)
                },
                session = {
                    secondSessionStarted = true
                }
            )
        }

        advanceUntilIdle()
        assertThat(firstSessionJob.isCompleted).isFalse()
        assertThat(secondSessionInitialized).isTrue()
        assertThat(secondSessionStarted).isFalse()

        // Allow the first session to finish cancelling.
        finalizer.complete()

        secondSessionJob.join()
        assertThat(firstSessionJob.isCompleted).isTrue()
        assertThat(secondSessionStarted).isTrue()
    }

    private fun Job.isChildOf(parent: Job): Boolean {
        val query = this
        // Breadth-first search of children, since Job has no api to get parents.
        val queue = ArrayDeque(parent.children.toList())
        while (queue.isNotEmpty()) {
            val job = queue.removeFirst()
            if (job === query) return true
            queue.addAll(job.children)
        }
        return false
    }

    private data class TestSession(
        val coroutineScope: CoroutineScope,
        var value: String = ""
    )

    private class TestException : RuntimeException()
}
