/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.viewfinder.view.internal.futures

import androidx.core.util.Preconditions
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Future

/** Utility class for generating specific implementations of [ListenableFuture]. */
object Futures {
    /**
     * Returns an implementation of [ListenableFuture] which immediately contains a result.
     *
     * @param value The result that is immediately set on the future.
     * @param <V> The type of the result.
     * @return A future which immediately contains the result. </V>
     */
    private fun <V> immediateFuture(value: V?): ListenableFuture<V> {
        return if (value == null) {
            ImmediateFuture.nullFuture()
        } else ImmediateFuture.ImmediateSuccessfulFuture(value)
    }

    /**
     * Returns a new `Future` whose result is asynchronously derived from the result of the given
     * `Future`. If the given `Future` fails, the returned `Future` fails with the same exception
     * (and the function is not invoked).
     *
     * @param input The future to transform
     * @param function A function to transform the result of the input future to the result of the
     *   output future
     * @param executor Executor to run the function in.
     * @return A future that holds result of the function (if the input succeeded) or the original
     *   input's failure (if not)
     */
    @JvmStatic
    internal fun <I, O> transformAsync(
        input: ListenableFuture<I>,
        function: AsyncFunction<in I, out O>,
        executor: Executor,
    ): ListenableFuture<O> {
        val output: ChainingListenableFuture<I, O> = ChainingListenableFuture(function, input)
        input.addListener(output, executor)
        return output
    }

    /**
     * Returns a new `Future` whose result is derived from the result of the given `Future`. If
     * `input` fails, the returned `Future` fails with the same exception (and the function is not
     * invoked)
     *
     * @param input The future to transform
     * @param function A function to transform the results of the provided future to the results of
     *   the returned future.
     * @param executor Executor to run the function in.
     * @return A future that holds result of the transformation.
     */
    @JvmStatic
    fun <I, O> transform(
        input: ListenableFuture<I>,
        function: (I?) -> O,
        executor: Executor,
    ): ListenableFuture<O> {
        Preconditions.checkNotNull(function)
        return transformAsync(input, { immediateFuture(function.invoke(it)) }, executor)
    }

    /**
     * Registers separate success and failure callbacks to be run when the `Future`'s computation is
     * [complete][Future.isDone] or, if the computation is already complete, immediately.
     *
     * @param future The future attach the callback to.
     * @param callback The callback to invoke when `future` is completed.
     * @param executor The executor to run `callback` when the future completes.
     */
    @JvmStatic
    fun <V> addCallback(
        future: ListenableFuture<V>,
        callback: FutureCallback<in V>,
        executor: Executor,
    ) {
        Preconditions.checkNotNull(callback)
        future.addListener(CallbackListener(future, callback), executor)
    }

    /**
     * Returns the result of the input `Future`, which must have already completed.
     *
     * The benefits of this method are twofold. First, the name "getDone" suggests to readers that
     * the `Future` is already done. Second, if buggy code calls `getDone` on a `Future` that is
     * still pending, the program will throw instead of block.
     *
     * @throws ExecutionException if the `Future` failed with an exception
     * @throws CancellationException if the `Future` was cancelled
     * @throws IllegalStateException if the `Future` is not done
     */
    @Throws(ExecutionException::class)
    private fun <V> getDone(future: Future<V>): V? {
        /*
         * We throw IllegalStateException, since the call could succeed later. Perhaps we
         * "should" throw IllegalArgumentException, since the call could succeed with a different
         * argument. Those exceptions' docs suggest that either is acceptable. Google's Java
         * Practices page recommends IllegalArgumentException here, in part to keep its
         * recommendation simple: Static methods should throw IllegalStateException only when
         * they use static state.
         *
         * Why do we deviate here? The answer: We want for fluentFuture.getDone() to throw the same
         * exception as Futures.getDone(fluentFuture).
         */
        Preconditions.checkState(future.isDone, "Future was expected to be done, $future")
        return getUninterruptibly(future)
    }

    /**
     * Invokes `Future.`[get()][Future.get] uninterruptibly.
     *
     * @throws ExecutionException if the computation threw an exception
     * @throws CancellationException if the computation was cancelled
     */
    @Throws(ExecutionException::class)
    internal fun <V> getUninterruptibly(future: Future<V>): V {
        var interrupted = false
        try {
            while (true) {
                interrupted =
                    try {
                        return future.get()
                    } catch (_: InterruptedException) {
                        true
                    }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt()
            }
        }
    }

    /** See [.addCallback] for behavioral notes. */
    private class CallbackListener<V>(val mFuture: Future<V>, val callback: FutureCallback<in V>) :
        Runnable {

        override fun run() {
            val value: V?
            try {
                value = getDone(mFuture)
            } catch (e: ExecutionException) {
                e.cause?.let { callback.onFailure(it) }
                return
            } catch (e: RuntimeException) {
                callback.onFailure(e)
                return
            } catch (e: Error) {
                callback.onFailure(e)
                return
            }
            callback.onSuccess(value)
        }

        override fun toString(): String {
            return this::class.simpleName + "," + callback
        }
    }
}
