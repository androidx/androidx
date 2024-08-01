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

package androidx.camera.impl.utils.futures

import androidx.arch.core.util.Function
import androidx.camera.impl.utils.executor.ViewfinderExecutors
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.util.Preconditions
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture

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
     * Returns an implementation of [ScheduledFuture] which immediately contains an exception that
     * will be thrown by [Future.get].
     *
     * @param cause The cause of the [ExecutionException] that will be thrown by [Future.get].
     * @param <V> The type of the result.
     * @return A future which immediately contains an exception. </V>
     */
    fun <V> immediateFailedScheduledFuture(cause: Throwable): ScheduledFuture<V> {
        return ImmediateFuture.ImmediateFailedScheduledFuture(cause)
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
    fun <I, O> transformAsync(
        input: ListenableFuture<I>,
        function: AsyncFunction<in I, out O>,
        executor: Executor
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
    fun <I, O> transform(
        input: ListenableFuture<I>,
        function: Function<in I?, out O>,
        executor: Executor
    ): ListenableFuture<O> {
        Preconditions.checkNotNull(function)
        return transformAsync(input, { immediateFuture(function.apply(it)) }, executor)
    }

    /**
     * Propagates the result of the given `ListenableFuture` to the given [ ] directly.
     *
     * If `input` fails, the failure will be propagated to the `completer`.
     *
     * @param input The future being propagated.
     * @param completer The completer which will receive the result of the provided future.
     */
    // ListenableFuture not needed for SAM conversion
    @JvmStatic
    fun <V> propagate(input: ListenableFuture<V>, completer: CallbackToFutureAdapter.Completer<V>) {
        // Use direct executor here since function is just unpacking the output and should be quick
        propagateTransform(
            input,
            { functionInput -> functionInput },
            completer,
            ViewfinderExecutors.directExecutor()
        )
    }

    /**
     * Propagates the result of the given `ListenableFuture` to the given [ ] by applying the
     * provided transformation function.
     *
     * If `input` fails, the failure will be propagated to the `completer` (and the function is not
     * invoked)
     *
     * @param input The future to transform.
     * @param function A function to transform the results of the provided future to the results of
     *   the provided completer.
     * @param completer The completer which will receive the result of the provided future.
     * @param executor Executor to run the function in.
     */
    private fun <I, O> propagateTransform(
        input: ListenableFuture<I>,
        function: Function<in I, out O>,
        completer: CallbackToFutureAdapter.Completer<O>,
        executor: Executor
    ) {
        propagateTransform(true, input, function, completer, executor)
    }

    /**
     * Propagates the result of the given `ListenableFuture` to the given [ ] by applying the
     * provided transformation function.
     *
     * If `input` fails, the failure will be propagated to the `completer` (and the function is not
     * invoked)
     *
     * @param propagateCancellation `true` to propagate the cancellation from completer to input
     *   future.
     * @param input The future to transform.
     * @param function A function to transform the results of the provided future to the results of
     *   the provided completer.
     * @param completer The completer which will receive the result of the provided future.
     * @param executor Executor to run the function in.
     */
    private fun <I, O> propagateTransform(
        propagateCancellation: Boolean,
        input: ListenableFuture<I>,
        function: Function<in I, out O>,
        completer: CallbackToFutureAdapter.Completer<O>,
        executor: Executor
    ) {
        Preconditions.checkNotNull(input)
        Preconditions.checkNotNull(function)
        Preconditions.checkNotNull(completer)
        Preconditions.checkNotNull(executor)
        addCallback(
            input,
            object : FutureCallback<I> {
                override fun onSuccess(result: I?) {
                    try {
                        completer.set(function.apply(result))
                    } catch (t: Throwable) {
                        completer.setException(t)
                    }
                }

                override fun onFailure(t: Throwable) {
                    completer.setException(t)
                }
            },
            executor
        )
        if (propagateCancellation) {
            // Propagate cancellation from completer to input future
            completer.addCancellationListener(
                { input.cancel(true) },
                ViewfinderExecutors.directExecutor()
            )
        }
    }

    /**
     * Returns a `ListenableFuture` whose result is set from the supplied future when it completes.
     *
     * Cancelling the supplied future will also cancel the returned future, but cancelling the
     * returned future will have no effect on the supplied future.
     */
    @JvmStatic
    fun <V> nonCancellationPropagating(future: ListenableFuture<V>): ListenableFuture<V> {
        Preconditions.checkNotNull(future)
        return if (future.isDone) {
            future
        } else
            CallbackToFutureAdapter.getFuture {
                // Input of function is same as output
                propagateTransform(
                    false,
                    future,
                    { input -> input },
                    it,
                    ViewfinderExecutors.directExecutor()
                )
                "nonCancellationPropagating[$future]"
            }
    }

    /**
     * Creates a new `ListenableFuture` whose value is a list containing the values of all its
     * successful input futures. The list of results is in the same order as the input list, and if
     * any of the provided futures fails or is canceled, its corresponding position will contain
     * `null` (which is indistinguishable from the future having a successful value of `null`).
     *
     * Canceling this future will attempt to cancel all the component futures.
     *
     * @param futures futures to combine
     * @return a future that provides a list of the results of the component futures
     */
    fun <V> successfulAsList(
        futures: Collection<ListenableFuture<out V>>
    ): ListenableFuture<List<V?>?> {
        return ListFuture(futures.toList(), false, ViewfinderExecutors.directExecutor())
    }

    /**
     * Creates a new `ListenableFuture` whose value is a list containing the values of all its input
     * futures, if all succeed.
     *
     * The list of results is in the same order as the input list.
     *
     * Canceling this future will attempt to cancel all the component futures, and if any of the
     * provided futures fails or is canceled, this one is, too.
     *
     * @param futures futures to combine
     * @return a future that provides a list of the results of the component futures
     */
    fun <V> allAsList(futures: Collection<ListenableFuture<out V>>): ListenableFuture<List<V?>?> {
        return ListFuture(futures.toList(), true, ViewfinderExecutors.directExecutor())
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
        executor: Executor
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
    fun <V> getDone(future: Future<V>): V? {
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
    fun <V> getUninterruptibly(future: Future<V>): V {
        var interrupted = false
        try {
            while (true) {
                interrupted =
                    try {
                        return future.get()
                    } catch (e: InterruptedException) {
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
    private class CallbackListener<V>(val mFuture: Future<V>, callback: FutureCallback<in V>) :
        Runnable {
        val mCallback: FutureCallback<in V>

        init {
            mCallback = callback
        }

        override fun run() {
            val value: V?
            try {
                value = getDone(mFuture)
            } catch (e: ExecutionException) {
                e.cause?.let { mCallback.onFailure(it) }
                return
            } catch (e: RuntimeException) {
                mCallback.onFailure(e)
                return
            } catch (e: Error) {
                mCallback.onFailure(e)
                return
            }
            mCallback.onSuccess(value)
        }

        override fun toString(): String {
            return javaClass.getSimpleName() + "," + mCallback
        }
    }
}
