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

package androidx.camera.camera2.pipe.core

import android.os.Handler
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * This collection pre-configured executors, dispatchers, and scopes that are used throughout this
 * library.
 */
public class Threads(
    public val cameraPipeScope: CoroutineScope,
    private val cameraPipeDispatchScope: CoroutineScope,
    public val blockingExecutor: Executor,
    public val blockingDispatcher: CoroutineDispatcher,
    public val backgroundExecutor: Executor,
    public val backgroundDispatcher: CoroutineDispatcher,
    public val lightweightExecutor: Executor,
    public val lightweightDispatcher: CoroutineDispatcher,
    camera2Handler: () -> Handler,
    camera2Executor: () -> Executor,
) {
    private val _camera2Handler = lazy { camera2Handler() }
    private val _camera2Executor = lazy { camera2Executor() }

    public val camera2Handler: Handler
        get() = _camera2Handler.value

    public val camera2Executor: Executor
        get() = _camera2Executor.value

    /**
     * runBlockingChecked runs the specified [block] on a timeout of [timeoutMs]. The function runs
     * the given block asynchronously on a supervised scope, allowing it to return after the timeout
     * completes, even if the calling thread is blocked. Throws [IllegalStateException] when the
     * execution of the [block] times out.
     */
    public fun <T> runBlockingChecked(timeoutMs: Long, block: suspend () -> T): T {
        return runBlocking(blockingDispatcher) {
            val result = runAsyncSupervised(backgroundDispatcher, block)
            try {
                withTimeout(timeoutMs) { result.await() }
            } catch (e: TimeoutCancellationException) {
                Log.error(e) { "Timed out after ${timeoutMs}ms!" }
                // For some reason, if TimeoutCancellationException is thrown, runBlocking can
                // suspend indefinitely. Catch it and rethrow IllegalStateException.
                throw IllegalStateException("Timed out after ${timeoutMs}ms!")
            }
        }
    }

    /**
     * runBlockingWithTimeOrNull runs the specified [block] on a timeout of [timeoutMs]. The
     * function runs the given block asynchronously on a supervised scope, allowing it to return
     * after the timeout completes, even if the calling thread is blocked. Returns null when the
     * execution of the [block] times out.
     */
    public fun <T> runBlockingCheckedOrNull(timeoutMs: Long, block: suspend () -> T): T? {
        return try {
            runBlocking(blockingDispatcher) {
                val result = runAsyncSupervised(backgroundDispatcher, block)
                withTimeoutOrNull(timeoutMs) { result.await() }
            }
        } catch (e: InterruptedException) {
            Log.info(e) { "runBlockingCheckedOrNull cancelled by thread interruption" }
            null
        }
    }

    private fun <T> runAsyncSupervised(
        dispatcher: CoroutineDispatcher,
        block: suspend () -> T,
    ): Deferred<T> {
        return cameraPipeDispatchScope.async(dispatcher) { block() }
    }
}
