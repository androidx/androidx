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

// TODO: Remove once paging-runtime is converted to .kt.
@file:JvmName("Futures")

package androidx.paging.futures

import androidx.annotation.RestrictTo
import androidx.arch.core.util.Function
import androidx.concurrent.futures.ResolvableFuture

import com.google.common.util.concurrent.ListenableFuture

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/**
 * Registers separate success and failure callbacks to be run when the `Future`'s computation is
 * complete or, if the computation is already complete, immediately.
 *
 * The callback is run on `executor`. There is no guaranteed ordering of execution of callbacks,
 * but any callback added through this method is guaranteed to be called once the computation is
 * complete.
 *
 * Example:
 * ```
 * ListenableFuture<QueryResult> future = ...;
 * Executor e = ...
 * addCallback(future, new FutureCallback<QueryResult>() {
 *     public void onSuccess(QueryResult result) {
 *         storeInCache(result);
 *     }
 *     public void onFailure(Throwable t) {
 *         reportError(t);
 *     }
 * }, e);
 * ```
 *
 * When selecting an executor, note that `directExecutor` is dangerous in some cases. See the
 * discussion in the [ListenableFuture.addListener] documentation. All its warnings about
 * heavyweight listeners are also applicable to heavyweight callbacks passed to this method.
 *
 * For a more general interface to attach a completion listener to a `Future`, see
 * [ListenableFuture.addListener].
 *
 * @param callback The callback to invoke when `future` is completed.
 * @param executor The executor to run `callback` when the future completes.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <V> ListenableFuture<out V>.addCallback(callback: FutureCallback<in V>, executor: Executor) {
    addListener(Runnable {
        val value: V
        try {
            value = get()
        } catch (e: ExecutionException) {
            callback.onError(e.cause ?: e)
            return@Runnable
        } catch (e: Throwable) {
            callback.onError(e)
            return@Runnable
        }

        callback.onSuccess(value)
    }, executor)
}

/**
 * Returns a new `Future` whose result is derived from the result of the given `Future`. If
 * `input` fails, the returned `Future` fails with the same exception (and the function is not
 * invoked). Example usage:
 *
 * ```
 * ListenableFuture<QueryResult> queryFuture = ...;
 * ListenableFuture<List<Row>> rowsFuture =
 *     transform(queryFuture, QueryResult::getRows, executor);
 * ```
 *
 * When selecting an executor, note that `directExecutor` is dangerous in some cases. See the
 * discussion in the [ListenableFuture.addListener] documentation. All its warnings about
 * heavyweight listeners are also applicable to heavyweight functions passed to this method.
 *
 * The returned `Future` attempts to keep its cancellation state in sync with that of the input
 * future. That is, if the returned `Future` is cancelled, it will attempt to cancel the input,
 * and if the input is cancelled, the returned `Future` will receive a callback in which it will
 * attempt to cancel itself.
 *
 * An example use of this method is to convert a serializable object returned from an RPC into a
 * POJO.
 *
 * @param function A Function to transform the results of the provided future to the results of
 *                 the returned future.
 * @param executor Executor to run the function in.
 * @return A future that holds result of the transformation.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <I, O> ListenableFuture<out I>.transform(
    function: Function<in I, out O>,
    executor: Executor
) = transform({ function.apply(it) }, executor)

internal inline fun <I, O> ListenableFuture<out I>.transform(
    crossinline function: (input: I) -> O,
    executor: Executor
): ListenableFuture<O> {
    val out = ResolvableFuture.create<O>()

    // Add success/error callback.
    addCallback(object : FutureCallback<I> {
        override fun onSuccess(value: I) {
            out.set(function(value))
        }

        override fun onError(throwable: Throwable) {
            out.setException(throwable)
        }
    }, executor)

    // Propagate output future's cancellation to input future.
    out.addCallback(object : FutureCallback<O> {
        override fun onSuccess(value: O) {}

        override fun onError(throwable: Throwable) {
            if (throwable is CancellationException) {
                cancel(false)
            }
        }
    }, executor)
    return out
}
