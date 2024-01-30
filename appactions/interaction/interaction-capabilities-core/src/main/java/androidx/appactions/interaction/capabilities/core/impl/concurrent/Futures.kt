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
package androidx.appactions.interaction.capabilities.core.impl.concurrent

import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.function.Function
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Future/ListenableFuture related utility methods.  */
object Futures {
    /** Attach a FutureCallback to a ListenableFuture instance.  */
    fun <V> addCallback(
        future: ListenableFuture<V>,
        callback: FutureCallback<in V>,
        executor: Executor
    ) {
        future.addListener(CallbackListener(future, callback), executor)
    }

    /**
     * Transforms an input ListenableFuture into a second ListenableFuture by applying a
     * transforming
     * function to the result of the input ListenableFuture.
     */
    fun <I, O> transform(
        input: ListenableFuture<I>,
        function: Function<I, O>,
        executor: Executor,
        tag: String?
    ): ListenableFuture<O> {
        return CallbackToFutureAdapter.getFuture {
            completer: CallbackToFutureAdapter.Completer<O> ->
            addCallback(input, transformFutureCallback(completer, function), executor)
            tag
        }
    }

    /**
     * Transforms an input ListenableFuture into a second ListenableFuture by applying an
     * asynchronous
     * transforming function to the result of the input ListenableFuture.
     */
    fun <I, O> transformAsync(
        input: ListenableFuture<I>,
        asyncFunction: Function<I, ListenableFuture<O>>,
        executor: Executor,
        tag: String
    ): ListenableFuture<O> {
        return CallbackToFutureAdapter.getFuture {
            completer: CallbackToFutureAdapter.Completer<O> ->
            addCallback(
                input, asyncTransformFutureCallback(completer, asyncFunction),
                executor
            )
            tag
        }
    }

    /** Returns a Future that is immediately complete with the given value.  */
    fun <V> immediateFuture(value: V): ListenableFuture<V> {
        return CallbackToFutureAdapter.getFuture {
            completer: CallbackToFutureAdapter.Completer<V> ->
            completer.set(
                value
            )
        }
    }

    /** Returns a Future that is immediately complete with null value.  */
    fun immediateVoidFuture(): ListenableFuture<Void> {
        return CallbackToFutureAdapter.getFuture {
            completer: CallbackToFutureAdapter.Completer<Void> ->
            completer.set(
                null
            )
        }
    }

    /** Returns a Future that is immediately complete with an exception.  */
    fun <V> immediateFailedFuture(throwable: Throwable): ListenableFuture<V> {
        return CallbackToFutureAdapter.getFuture {
            completer: CallbackToFutureAdapter.Completer<V> ->
            completer.setException(
                throwable
            )
        }
    }

    /**
     * Returns a FutureCallback that transform the result in onSuccess, and then set the result in
     * completer.
     */
    fun <I, O> transformFutureCallback(
        completer: CallbackToFutureAdapter.Completer<O>,
        function: Function<I, O>
    ): FutureCallback<I> {
        return object : FutureCallback<I> {
            override fun onSuccess(result: I) {
                try {
                    completer.set(function.apply(result))
                } catch (t: Throwable) {
                    if (t is InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                    completer.setException(t)
                }
            }

            override fun onFailure(t: Throwable) {
                completer.setException(t)
            }
        }
    }

    /** Returns a FutureCallback that asynchronously transform the result.  */
    private fun <I, O> asyncTransformFutureCallback(
        completer: CallbackToFutureAdapter.Completer<O>,
        asyncFunction: Function<I, ListenableFuture<O>>
    ): FutureCallback<I> {
        return object : FutureCallback<I> {
            override fun onSuccess(result: I) {
                try {
                    addCallback(
                        asyncFunction.apply(result),
                        transformFutureCallback(completer, Function.identity())
                    ) { obj: Runnable -> obj.run() }
                } catch (t: Throwable) {
                    if (t is InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                    completer.setException(t)
                }
            }

            override fun onFailure(t: Throwable) {
                completer.setException(t)
            }
        }
    }

    @Throws(ExecutionException::class)
    fun <V> getDone(future: Future<V>): V {
        if (!future.isDone) {
            throw IllegalStateException("future is expected to be done already.")
        }
        var interrupted = false
        try {
            while (true) {
                interrupted = try {
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

    private class CallbackListener<V> internal constructor(
        val mFuture: Future<V>,
        val mCallback: FutureCallback<in V>
    ) : Runnable {
        override fun run() {
            val value: V
            value = try {
                getDone(mFuture)
            } catch (e: ExecutionException) {
                val cause = e.cause
                mCallback.onFailure(cause ?: e)
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
    }
}

fun <T> convertToListenableFuture(
    tag: String,
    block: suspend CoroutineScope.() -> T,
): ListenableFuture<T> {
    return CallbackToFutureAdapter.getFuture { completer ->
        val job = CoroutineScope(Dispatchers.Unconfined).launch {
                try {
                    completer.set(block())
                } catch (t: Throwable) {
                    completer.setException(t)
                }
            }
        completer.addCancellationListener(
            { job.cancel() },
            Runnable::run,
        )
        "ListenableFutureHelper#convertToListenableFuture for '$tag'"
    }
}
