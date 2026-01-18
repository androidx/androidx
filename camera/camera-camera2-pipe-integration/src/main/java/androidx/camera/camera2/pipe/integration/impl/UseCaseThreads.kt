/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import java.util.concurrent.Executor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

/** Collection of threads and scope(s) that have been configured and tuned. */
public class UseCaseThreads(
    public val scope: CoroutineScope,
    public val backgroundExecutor: Executor,
    public val backgroundDispatcher: CoroutineDispatcher,
) {
    public val mainHandler: Handler = Handler(Looper.getMainLooper())
    private val sequentialExecutorDelegate =
        CameraXExecutors.newSequentialExecutor(backgroundExecutor)

    /**
     * A thread-local flag to identify if the current thread is executing a task for this specific
     * instance of [UseCaseThreads].
     */
    private val isSequentialThread = ThreadLocal<Boolean>()

    /**
     * Returns true if the current thread is the one associated with the [sequentialScope].
     *
     * This is useful for determining dispatching strategies (e.g., UNDISPATCHED) without the
     * performance overhead of exception handling.
     */
    public fun isOnSequentialThread(): Boolean = isSequentialThread.get() == true

    /**
     * Executor that enforces sequential execution and sets the [isSequentialThread] flag during
     * task execution.
     */
    public val sequentialExecutor: Executor = Executor { command ->
        sequentialExecutorDelegate.execute {
            isSequentialThread.set(true)
            try {
                command.run()
            } finally {
                isSequentialThread.remove()
            }
        }
    }
    private val sequentialDispatcher = sequentialExecutor.asCoroutineDispatcher()
    public var sequentialScope: CoroutineScope =
        CoroutineScope(scope.coroutineContext + SupervisorJob() + sequentialDispatcher)
        @VisibleForTesting set

    /**
     * Verifies that the current thread is the one associated with the [sequentialScope].
     *
     * This method is critical for functions using [kotlinx.coroutines.CoroutineStart.UNDISPATCHED]
     * to ensure they are safely accessing state confined to the sequential thread.
     *
     * @throws IllegalStateException if called from a different thread context.
     */
    public fun checkOnSequentialThread() {
        check(isOnSequentialThread()) {
            "Thread check failed: This method must be called from the UseCaseThreads sequential " +
                "scope. Current thread: ${Thread.currentThread().name}"
        }
    }

    /**
     * Confines a [Deferred] returning `block` parameter to [UseCaseThreads.sequentialScope] for the
     * purpose of thread confinement.
     *
     * Cancelling the `Deferred` returned from this function does not cancel the `Deferred` returned
     * by `block`.
     *
     * @return A [Deferred] which is completed as per the [Deferred] returned from [block] via
     *   [propagateTo].
     */
    public inline fun <T> confineDeferred(crossinline block: () -> Deferred<T>): Deferred<T> {
        val signal = CompletableDeferred<T>()
        sequentialScope.launch { block().propagateTo(signal) }
        return signal
    }

    /**
     * Confines a [Deferred] list returning `block` parameter to [UseCaseThreads.sequentialScope]
     * for the purpose of thread confinement and returns a new `Deferred` list with one-to-one
     * mapping.
     *
     * Cancelling a `Deferred` returned from this function does not cancel the corresponding
     * `Deferred` returned by `block`.
     *
     * @param size Size of the list returned from [block], the list returned via this function will
     *   also have th same size.
     * @return A list of [Deferred] where each element is completed as per the corresponding
     *   [Deferred] in the list returned from [block].
     */
    public inline fun <T> confineDeferredList(
        size: Int,
        crossinline block: () -> List<Deferred<T>>,
    ): List<Deferred<T>> {
        val deferredList = List(size) { CompletableDeferred<T>() }
        sequentialScope.launch {
            block().forEachIndexed { index, deferred -> deferred.propagateTo(deferredList[index]) }
        }
        return deferredList
    }

    /**
     * Confines a [Deferred] returning suspendable `block` parameter to
     * [UseCaseThreads.sequentialScope] for the purpose of thread confinement.
     *
     * Cancelling the `Deferred` returned from this function does not cancel the `Deferred` returned
     * by `block`.
     *
     * @param block The suspend lambda that returns a [Deferred].
     * @param start The start option for the coroutine. Defaults to [CoroutineStart.DEFAULT].
     *   **Note:** Do not use [CoroutineStart.LAZY] as the internal [Job] is not returned, meaning
     *   the coroutine cannot be started manually.
     * @return A [Deferred] which is completed as per the [Deferred] returned from [block] via
     *   [propagateTo].
     */
    public inline fun <T> confineDeferredSuspend(
        crossinline block: suspend () -> Deferred<T>,
        start: CoroutineStart = CoroutineStart.DEFAULT,
    ): Deferred<T> {
        val signal = CompletableDeferred<T>()
        sequentialScope.launch(start = start) { block().propagateTo(signal) }
        return signal
    }

    /**
     * Confines a [Deferred] list returning suspendable [block] parameter to
     * [UseCaseThreads.sequentialScope] for the purpose of thread confinement and returns a new
     * [Deferred] list with one-to-one mapping.
     *
     * Cancelling a [Deferred] returned from this function does not cancel the corresponding
     * [Deferred] returned by [block].
     *
     * @param size Size of the list returned from [block], the list returned via this function will
     *   also have the same size.
     * @param start Coroutine starting strategy (default: [CoroutineStart.DEFAULT]). Use
     *   [CoroutineStart.UNDISPATCHED] to start execution immediately on the current thread if the
     *   caller is already on the sequential thread.
     * @return A list of [Deferred] where each element is completed as per the corresponding
     *   [Deferred] in the list returned from [block].
     */
    public inline fun <T> confineDeferredListSuspend(
        size: Int,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        crossinline block: suspend () -> List<Deferred<T>>,
    ): List<Deferred<T>> {
        val deferredList = List(size) { CompletableDeferred<T>() }
        sequentialScope.launch(start = start) {
            block().forEachIndexed { index, deferred -> deferred.propagateTo(deferredList[index]) }
        }
        return deferredList
    }

    /**
     * Confines the `block` parameter to [UseCaseThreads.sequentialScope] for the purpose of thread
     * confinement.
     *
     * This is mainly just a syntax sugar matching [CoroutineScope.launch] to align with the other
     * confining methods in this class.
     *
     * @return A [Job] which is returned via [CoroutineScope.launch] used under-the-hood.
     */
    public inline fun confineLaunch(crossinline block: suspend () -> Unit): Job =
        sequentialScope.launch { block() }

    // TODO - Add convenience function like confineAsync to mimic scope.async while still following
    //  the above pattern when being used for thread confinement. This will keep things clear if we
    //  want to replace sequentialScope with something else in future for thread confinement.
}
