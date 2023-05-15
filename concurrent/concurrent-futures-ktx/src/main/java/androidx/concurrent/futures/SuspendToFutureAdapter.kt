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

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * A utility for launching suspending calls scoped and managed by a returned [ListenableFuture],
 * used for adapting Kotlin suspending APIs to be callable from the Java programming language.
 */
public object SuspendToFutureAdapter {

    private val GlobalListenableFutureScope = CoroutineScope(Dispatchers.Main)
    private val GlobalListenableFutureAwaitContext = Dispatchers.Unconfined

    /**
     * Launch [block] in [context], returning a [ListenableFuture] to manage the launched operation.
     * [block] will run **synchronously** to its first suspend point, behaving as
     * [CoroutineStart.UNDISPATCHED] by default; set [launchUndispatched] to false to override
     * and behave as [CoroutineStart.DEFAULT].
     *
     * [launchFuture] can be used to write adapters for calling suspending functions from the
     * Java programming language, e.g.
     *
     * ```
     * @file:JvmName("FancyServices")
     *
     * fun FancyService.requestAsync(
     *     args: FancyServiceArgs
     * ): ListenableFuture<FancyResult> = SuspendToFutureAdapter.launchFuture {
     *     request(args)
     * }
     * ```
     *
     * which can be called from Java language source code as follows:
     * ```
     * final ListenableFuture<FancyResult> result = FancyServices.requestAsync(service, args);
     * ```
     *
     * If no [kotlinx.coroutines.CoroutineDispatcher] is provided in [context], [Dispatchers.Main]
     * is used as the default. [ListenableFuture.get] should not be called from the main thread
     * prior to the future's completion (whether it was obtained from [SuspendToFutureAdapter]
     * or not) as any operation performed in the process of completing the future may require
     * main thread event processing in order to proceed, leading to potential main thread deadlock.
     *
     * If the operation performed by [block] is known to be safe for potentially reentrant
     * continuation resumption, immediate dispatchers such as [Dispatchers.Unconfined] may be used
     * as part of [context] to avoid additional thread dispatch latency. This should not be used
     * as a means of supporting clients blocking the main thread using [ListenableFuture.get];
     * this support can be broken by valid internal implementation changes to any transitive
     * dependencies of the operation performed by [block].
     */
    @Suppress("AsyncSuffixFuture")
    public fun <T> launchFuture(
        context: CoroutineContext = EmptyCoroutineContext,
        launchUndispatched: Boolean = true,
        block: suspend CoroutineScope.() -> T,
    ): ListenableFuture<T> {
        val resultDeferred = GlobalListenableFutureScope.async(
            context = context,
            start = if (launchUndispatched) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT,
            block = block
        )
        return DeferredFuture(resultDeferred).also { future ->
            // Deferred.getCompleted is marked experimental, so external libraries can't rely on it.
            // Instead, use await in a raw coroutine that will invoke [resumeWith] when it returns
            // using the Unconfined dispatcher.
            resultDeferred::await.createCoroutine(future).resume(Unit)
        }
    }

    private class DeferredFuture<T>(
        private val resultDeferred: Deferred<T>
    ) : ListenableFuture<T>, Continuation<T> {

        private val delegateFuture = ResolvableFuture.create<T>()

        // Implements external cancellation, propagating the cancel request to resultDeferred.
        // delegateFuture will be cancelled if resultDeferred becomes cancelled for
        // internal cancellation.
        override fun cancel(shouldInterrupt: Boolean): Boolean =
            delegateFuture.cancel(shouldInterrupt).also { didCancel ->
                if (didCancel) {
                    resultDeferred.cancel()
                }
            }

        override fun isCancelled(): Boolean = delegateFuture.isCancelled

        override fun isDone(): Boolean = delegateFuture.isDone

        override fun get(): T = delegateFuture.get()

        override fun get(timeout: Long, unit: TimeUnit): T = delegateFuture.get(timeout, unit)

        override fun addListener(listener: Runnable, executor: Executor) =
            delegateFuture.addListener(listener, executor)

        override val context: CoroutineContext
            get() = GlobalListenableFutureAwaitContext

        /**
         * Implementation of [Continuation] that will resume for the raw call to await
         * to resolve the [delegateFuture]
         */
        override fun resumeWith(result: Result<T>) {
            result.fold(
                onSuccess = {
                    delegateFuture.set(it)
                },
                onFailure = {
                    if (it is CancellationException) {
                        delegateFuture.cancel(false)
                    } else {
                        delegateFuture.setException(it)
                    }
                }
            )
        }
    }
}