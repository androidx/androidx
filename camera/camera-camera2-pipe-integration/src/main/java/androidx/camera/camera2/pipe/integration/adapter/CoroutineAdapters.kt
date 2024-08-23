/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Convert a job into a ListenableFuture<Void>.
 *
 * The return value of the Future is null, and canceling the future will not cancel the Job. The tag
 * field may be used to help debug futures.
 */
public fun Job.asListenableFuture(tag: Any? = "Job.asListenableFuture"): ListenableFuture<Void> {
    val resolver: CallbackToFutureAdapter.Resolver<Void> =
        CallbackToFutureAdapter.Resolver<Void> { completer ->
            this.invokeOnCompletion {
                if (it != null) {
                    if (it is CancellationException) {
                        completer.setCancelled()
                    } else {
                        completer.setException(it)
                    }
                } else {
                    completer.set(null)
                }
            }
            tag
        }
    return CallbackToFutureAdapter.getFuture(resolver)
}

/** Convert a job into a ListenableFuture<T>. */
@OptIn(ExperimentalCoroutinesApi::class)
public fun <T> Deferred<T>.asListenableFuture(
    tag: Any? = "Deferred.asListenableFuture"
): ListenableFuture<T> {
    val resolver: CallbackToFutureAdapter.Resolver<T> =
        CallbackToFutureAdapter.Resolver<T> { completer ->
            this.invokeOnCompletion {
                if (it != null) {
                    if (it is CancellationException) {
                        completer.setCancelled()
                    } else {
                        completer.setException(it)
                    }
                } else {
                    // Ignore exceptions - This should never throw in this situation.
                    completer.set(this.getCompleted())
                }
            }
            tag
        }
    return CallbackToFutureAdapter.getFuture(resolver)
}

/**
 * Propagates the result of this to `destination` parameter when this deferred is completed.
 *
 * Cancelling the destination is no-op returned from this function does not cancel the `Deferred`
 * returned by `block`.
 */
public fun <T> Deferred<T>.propagateTo(destination: CompletableDeferred<T>) {
    invokeOnCompletion { propagateCompletion(destination, it) }
}

/**
 * Propagates the result of this to `destination` parameter when this deferred is completed.
 *
 * Cancelling the destination is no-op returned from this function does not cancel the `Deferred`
 * returned by `block`.
 *
 * @param destination The destination [CompletableDeferred] to which result is propagated to.
 * @param transform Transformation function to convert the result during propagation.
 */
public fun <T, R> Deferred<T>.propagateTo(
    destination: CompletableDeferred<R>,
    transform: (T) -> R,
) {
    invokeOnCompletion { propagateCompletion(destination, it, transform) }
}

/**
 * Propagates the result of this to `destination` parameter immediately.
 *
 * This function assumes that [Deferred.invokeOnCompletion] has already been invoked.
 *
 * @param destination The destination `Deferred` to which result is propagated to.
 * @param completionCause The `Throwable` cause of completion that was passed in
 *   `Deferred.invokeOnCompletion`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun <T> Deferred<T>.propagateCompletion(
    destination: CompletableDeferred<T>,
    completionCause: Throwable?,
) {
    if (completionCause != null) {
        destination.completeFailing(completionCause)
    } else {
        // Ignore exceptions - This should never throw in this situation.
        destination.complete(getCompleted())
    }
}

/**
 * Propagates the result of this to `destination` parameter immediately.
 *
 * This function assumes that [Deferred.invokeOnCompletion] has already been invoked.
 *
 * @param destination The destination `Deferred` to which result is propagated to.
 * @param completionCause The `Throwable` cause of completion that was passed in
 *   `Deferred.invokeOnCompletion`.
 * @param transform Transformation function to convert the result during propagation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun <T, R> Deferred<T>.propagateCompletion(
    destination: CompletableDeferred<R>,
    completionCause: Throwable?,
    transform: (T) -> R,
) {
    if (completionCause != null) {
        destination.completeFailing(completionCause)
    } else {
        // Ignore exceptions - This should never throw in this situation.
        destination.complete(transform(getCompleted()))
    }
}

/**
 * Completes this `Deferred` as failure based on the provided `cause`.
 *
 * @param cause If it's an instance of [CancellationException], [Deferred.cancel] is invoked for
 *   this, otherwise, [CompletableDeferred.completeExceptionally] is invoked.
 */
public fun <T> CompletableDeferred<T>.completeFailing(
    cause: Throwable,
) {
    if (cause is CancellationException) {
        cancel(cause)
    } else {
        completeExceptionally(cause)
    }
}

/**
 * Waits for [Deferred.await] to be completed until the given timeout.
 *
 * @return true if `Deferred.await` had completed, false otherwise.
 */
public suspend fun <T> Deferred<T>.awaitUntil(timeoutMillis: Long): Boolean =
    withTimeoutOrNull(timeoutMillis) { this@awaitUntil.await() }?.let { true } ?: false
