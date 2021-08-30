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

package androidx.concurrent.futures

import androidx.concurrent.futures.AbstractResolvableFuture.getUninterruptibly
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import kotlin.coroutines.resumeWithException

/**
 * Awaits completion of `this` [ListenableFuture] without blocking a thread.
 *
 * This suspend function is cancellable.
 *
 * If the [kotlinx.coroutines.Job] of the current coroutine is cancelled or completed while this
 * suspending function is
 * waiting, this function stops waiting for the future and immediately resumes with
 * [CancellationException][kotlinx.coroutines.CancellationException].
 *
 * This method is intended to be used with one-shot Futures, so on coroutine cancellation, the
 * Future is cancelled as well. If cancelling the given future is undesired, use
 * [kotlinx.coroutines.NonCancellable].
 */
public suspend fun <T> ListenableFuture<T>.await(): T {
    try {
        if (isDone) return getUninterruptibly(this)
    } catch (e: ExecutionException) {
        // ExecutionException is the only kind of exception that can be thrown from a gotten
        // Future, other than CancellationException. Cancellation is propagated upward so that
        // the coroutine running this suspend function may process it.
        // Any other Exception showing up here indicates a very fundamental bug in a
        // Future implementation.
        throw e.nonNullCause()
    }

    return suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        addListener(
            ToContinuation(this, cont),
            DirectExecutor.INSTANCE
        )
        cont.invokeOnCancellation {
            cancel(false)
        }
    }
}

/**
 * Propagates the outcome of [futureToObserve] to [continuation] on completion.
 *
 * Cancellation is propagated as cancelling the continuation. If [futureToObserve] completes
 * and fails, the cause of the Future will be propagated without a wrapping
 * [ExecutionException] when thrown.
 */
private class ToContinuation<T>(
    val futureToObserve: ListenableFuture<T>,
    val continuation: CancellableContinuation<T>
) : Runnable {
    override fun run() {
        if (futureToObserve.isCancelled) {
            continuation.cancel()
        } else {
            try {
                continuation.resumeWith(
                    Result.success(getUninterruptibly(futureToObserve))
                )
            } catch (e: ExecutionException) {
                // ExecutionException is the only kind of exception that can be thrown from a gotten
                // Future. Anything else showing up here indicates a very fundamental bug in a
                // Future implementation.
                continuation.resumeWithException(e.nonNullCause())
            }
        }
    }
}

/**
 * Returns the cause from an [ExecutionException] thrown by a [Future.get] or similar.
 *
 * [ExecutionException] _always_ wraps a non-null cause when Future.get() throws. A Future cannot
 * fail without a non-null `cause`, because the only way a Future _can_ fail is an uncaught
 * [Exception].
 *
 * If this !! throws [NullPointerException], a Future is breaking its interface contract and losing
 * state - a serious fundamental bug.
 */
internal fun ExecutionException.nonNullCause(): Throwable {
    return this.cause!!
}
