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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.concurrent.futures

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

@RunWith(JUnit4::class)
@OptIn(DelicateCoroutinesApi::class)
class ListenableFutureTest {
    private var actionIndex = AtomicInteger()
    private var finished = AtomicBoolean()

    @Test
    fun testFutureWithResult() {
        val future: ResolvableFuture<Int> = ResolvableFuture.create()
        val job = GlobalScope.launch {
            val result = future.await()
            assertThat(result, `is`(10))
        }
        future.set(10)
        runBlocking {
            job.join()
        }
    }

    @Test
    fun testFutureWithException() {
        val future: ResolvableFuture<Int> = ResolvableFuture.create()
        val exception = RuntimeException("Something bad happened")
        val job = GlobalScope.launch {
            try {
                future.await()
            } catch (throwable: Throwable) {
                assertThat(throwable, `is`(instanceOf(RuntimeException::class.java)))
                assertThat(throwable.message, `is`(exception.message))
            }
        }
        future.setException(exception)
        runBlocking {
            job.join()
        }
    }

    @Test
    fun testFutureCancellation() {
        val future: ResolvableFuture<Int> = ResolvableFuture.create()
        val job = GlobalScope.launch {
            future.await()
        }
        future.cancel(true)
        runBlocking {
            job.join()
            assertThat(job.isCancelled, `is`(true))
        }
    }

    @Test
    fun testAwaitWithCancellation() = runBlockingTest {
        val future = ResolvableFuture.create<Int>()
        val deferred = async {
            future.await()
        }

        deferred.cancel(TestCancellationException())
        assertFailsWith<TestCancellationException> {
            deferred.await()
            expectUnreached()
        }
    }

    @Test
    fun testCancellableAwait() = runBlocking {
        expect(1)
        val toAwait = ResolvableFuture.create<String>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            expect(2)
            try {
                toAwait.await() // suspends
            } catch (e: CancellationException) {
                expect(5) // should throw cancellation exception
                throw e
            }
        }
        expect(3)
        job.cancel() // cancel the job
        toAwait.set("fail") // too late, the waiting job was already cancelled
        expect(4) // job processing of cancellation was scheduled, not executed yet
        yield() // yield main thread to job
        finish(6)
    }

    /**
     * Asserts that this invocation is `index`-th in the execution sequence (counting from one).
     */
    private fun expect(index: Int) {
        val wasIndex = actionIndex.incrementAndGet()
        check(index == wasIndex) { "Expecting action index $index but it is actually $wasIndex" }
    }

    /**
     * Asserts that this it the last action in the test. It must be invoked by any test that used
     * [expect].
     */
    private fun finish(index: Int) {
        expect(index)
        check(!finished.getAndSet(true)) { "Should call 'finish(...)' at most once" }
    }

    /**
     * Asserts that this line is never executed.
     */
    private fun expectUnreached() {
        error("Should not be reached")
    }

    private class TestCancellationException : CancellationException()
}
