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

package androidx.concurrent.futures

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SuspendToFutureAdapterTest {

    @Test
    fun completeNormally() {
        val expected = Any()
        val completer = CompletableDeferred<Any>()
        val future = SuspendToFutureAdapter.launchFuture(Dispatchers.Unconfined) {
            completer.await()
        }

        assertWithMessage("isDone before completion").that(future.isDone).isFalse()
        completer.complete(expected)
        assertWithMessage("isDone after completion").that(future.isDone).isTrue()
        assertWithMessage("isCancelled").that(future.isCancelled).isFalse()
        assertWithMessage("get").that(future.get()).isSameInstanceAs(expected)
    }

    @Test
    fun completeWithException() {
        val completer = CompletableDeferred<Any>()
        val future = SuspendToFutureAdapter.launchFuture(Dispatchers.Unconfined) {
            completer.await()
        }

        assertWithMessage("isDone before completion").that(future.isDone).isFalse()
        // Note: anonymous subclass object used here so as to defeat kotlinx.coroutines'
        // debug behavior of attempting to reflectively clone exceptions to recover stack traces
        val exception = object : RuntimeException("expected exception") {}
        completer.completeExceptionally(exception)
        assertWithMessage("isDone after completion").that(future.isDone).isTrue()
        assertWithMessage("isCancelled").that(future.isCancelled).isFalse()
        val result = runCatching { future.get() }
        assertWithMessage("result failed").that(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull())
            .isInstanceOf(ExecutionException::class.java)
        assertThat(result.exceptionOrNull())
            .hasCauseThat()
            .isSameInstanceAs(exception)
    }

    @Test
    fun cancelledInternally() {
        val future = SuspendToFutureAdapter.launchFuture(Dispatchers.Unconfined) {
            throw CancellationException("internal cancellation")
        }
        assertThat(future.isDone).isTrue()
        assertThat(future.isCancelled).isTrue()
        assertThat(runCatching { future.get() }.exceptionOrNull())
            .isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun cancelledExternally() {
        var cancellationException: CancellationException? = null
        val future = SuspendToFutureAdapter.launchFuture(Dispatchers.Unconfined) {
            try {
                awaitCancellation()
            } catch (ce: CancellationException) {
                cancellationException = ce
            }
        }
        assertWithMessage("isDone before completion").that(future.isDone).isFalse()
        assertWithMessage("cancel returned").that(future.cancel(true)).isTrue()
        assertWithMessage("isDone after completion").that(future.isDone).isTrue()
        assertWithMessage("isCancelled").that(future.isCancelled).isTrue()
        assertThat(runCatching { future.get() }.exceptionOrNull())
            .isInstanceOf(CancellationException::class.java)
        assertWithMessage("coroutine caught exception")
            .that(cancellationException)
            .isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun externalCancelAfterCompletion() {
        val future = SuspendToFutureAdapter.launchFuture(Dispatchers.Unconfined) {}
        assertWithMessage("isDone").that(future.isDone).isTrue()
        assertWithMessage("cancel returned").that(future.cancel(true)).isFalse()
        assertWithMessage("isCancelled").that(future.isCancelled).isFalse()
    }

    @Test
    fun multipleExternalCancellation() {
        val future = SuspendToFutureAdapter.launchFuture(Dispatchers.Unconfined) {
            awaitCancellation()
        }
        assertWithMessage("isDone before cancel").that(future.isDone).isFalse()
        assertWithMessage("cancel 1").that(future.cancel(true)).isTrue()
        assertWithMessage("isDone after cancel 1").that(future.isDone).isTrue()
        assertWithMessage("isCancelled after cancel 1").that(future.isCancelled).isTrue()
        assertWithMessage("cancel 2").that(future.cancel(true)).isFalse()
        assertWithMessage("isDone after cancel 2").that(future.isDone).isTrue()
        assertWithMessage("isCancelled after cancel 1").that(future.isCancelled).isTrue()
    }

    @Test
    fun mainDispatcherIsDefault() {
        val future = SuspendToFutureAdapter.launchFuture {
            coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
        }
        assertWithMessage("observed dispatcher")
            .that(future.get()).isSameInstanceAs(Dispatchers.Main)
    }

    @Test
    fun noDispatchForNoSuspend() {
        val expected = Any()
        val dispatcher = CountingDispatcher()
        val future = SuspendToFutureAdapter.launchFuture(dispatcher) {
            expected
        }
        assertWithMessage("isDone")
            .that(future.isDone).isTrue()
        assertWithMessage("future value").that(future.get()).isSameInstanceAs(expected)

        // We should get zero dispatches if we do not suspend
        assertWithMessage("dispatch count").that(dispatcher.count).isEqualTo(0)
    }

    @Test
    fun noDispatchForImmediateResume() {
        val dispatcher = CountingDispatcher()
        val future = SuspendToFutureAdapter.launchFuture(dispatcher) {
            suspendCancellableCoroutine {
                it.resume(Unit)
            }
        }
        assertWithMessage("isDone")
            .that(future.isDone).isTrue()
        assertWithMessage("future value").that(future.get()).isSameInstanceAs(Unit)

        // We should get zero dispatches; resuming a continuation synchronously in
        // suspendCancellableCoroutine does not suspend
        assertWithMessage("dispatch count").that(dispatcher.count).isEqualTo(0)
    }

    @Test
    fun avoidAdditionalDispatch() {
        val dispatcher = CountingDispatcher()
        lateinit var continuation: Continuation<Int>
        val future = SuspendToFutureAdapter.launchFuture(dispatcher) {
            suspendCancellableCoroutine {
                continuation = it
            }
        }

        assertWithMessage("future isDone before continuation resume")
            .that(future.isDone).isFalse()

        continuation.resume(5)

        assertWithMessage("future isDone immediately after continuation resume")
            .that(future.isDone).isTrue()
        assertWithMessage("future value").that(future.get()).isEqualTo(5)

        // We shouldn't get more than one dispatch: from when suspendCancellableCoroutine resumes.
        // Maybe someday continuations will get tail call-like optimizations and the final resume
        // won't need it.
        assertWithMessage("dispatch count").that(dispatcher.count).isAtMost(1)
    }

    @Test
    fun dispatchForDisabledUndispatchedLaunch() {
        val expected = Any()
        val dispatcher = CountingDispatcher()
        val future = SuspendToFutureAdapter.launchFuture(
            dispatcher,
            launchUndispatched = false
        ) {
            expected
        }
        assertWithMessage("isDone")
            .that(future.isDone).isTrue()
        assertWithMessage("future value").that(future.get()).isSameInstanceAs(expected)

        // We should get one dispatch from disabling the default undispatched launch
        assertWithMessage("dispatch count").that(dispatcher.count).isEqualTo(1)
    }

    private class CountingDispatcher : CoroutineDispatcher() {
        private val _count = AtomicInteger(0)
        val count: Int
            get() = _count.get()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            _count.incrementAndGet()
            block.run()
        }
    }
}