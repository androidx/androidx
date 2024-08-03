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
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.util.Preconditions
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A [ListenableFuture] that supports chains of operations. For example:
 * <pre>`ListenableFuture<Boolean> adminIsLoggedIn =
 * FutureChain.from(usersDatabase.getAdminUser())
 * .transform(User::getId, directExecutor())
 * .transform(ActivityService::isLoggedIn, threadPool);
 * `</pre> *
 *
 * @param <V> </V>
 */
open class FutureChain<V> : ListenableFuture<V> {
    private val mDelegate: ListenableFuture<V>
    private var mCompleter: CallbackToFutureAdapter.Completer<V>? = null

    /**
     * Returns a new `Future` whose result is derived from the result of this `Future`. If this
     * input `Future` fails, the returned `Future` fails with the same exception (and the function
     * is not invoked).
     *
     * @param function A Function to transform the results of this future to the results of the
     *   returned future.
     * @param executor Executor to run the function in.
     * @return A future that holds result of the transformation.
     */
    fun <T> transform(function: Function<in V?, out T>, executor: Executor): FutureChain<T> {
        return Futures.transform(this, function, executor) as FutureChain<T>
    }

    internal constructor(delegate: ListenableFuture<V>) {
        mDelegate = Preconditions.checkNotNull(delegate)
    }

    internal constructor() {
        mDelegate =
            CallbackToFutureAdapter.getFuture { completer ->
                Preconditions.checkState(mCompleter == null, "The result can only set once!")
                mCompleter = completer
                "FutureChain[" + this@FutureChain + "]"
            }
    }

    override fun addListener(listener: Runnable, executor: Executor) {
        mDelegate.addListener(listener, executor)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return mDelegate.cancel(mayInterruptIfRunning)
    }

    override fun isCancelled(): Boolean {
        return mDelegate.isCancelled
    }

    override fun isDone(): Boolean {
        return mDelegate.isDone
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): V? {
        return mDelegate.get()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): V? {
        return mDelegate[timeout, unit]
    }

    fun set(value: V?): Boolean {
        return if (mCompleter != null) {
            mCompleter!!.set(value)
        } else false
    }

    fun setException(throwable: Throwable): Boolean {
        return if (mCompleter != null) {
            mCompleter!!.setException(throwable)
        } else false
    }

    companion object {
        /**
         * Converts the given `ListenableFuture` to an equivalent `FutureChain`.
         *
         * If the given `ListenableFuture` is already a `FutureChain`, it is returned directly. If
         * not, it is wrapped in a `FutureChain` that delegates all calls to the original
         * `ListenableFuture`.
         *
         * @return directly if input a FutureChain or a ListenableFuture wrapped by FutureChain.
         */
        fun <V> from(future: ListenableFuture<V>): FutureChain<V> {
            return if (future is FutureChain<*>) future as FutureChain<V> else FutureChain(future)
        }
    }
}
