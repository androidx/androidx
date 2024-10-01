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

package androidx.camera.camera2.pipe.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

internal object Threading {
    private val globalSupervisorScope =
        CoroutineScope(CoroutineName("GlobalThreadingScope") + SupervisorJob())

    /**
     * runBlockingChecked runs the specified [block] on a timeout of [timeoutMs] using the given
     * [dispatcher]. The function runs the given block asynchronously on a supervised scope,
     * allowing it to return after the timeout completes, even if the calling thread is blocked.
     * Throws [IllegalStateException] when the execution of the [block] times out.
     */
    fun <T> runBlockingChecked(
        dispatcher: CoroutineDispatcher,
        timeoutMs: Long,
        block: suspend () -> T
    ): T {
        return runBlocking {
            val result = runAsyncSupervised(dispatcher, block)
            try {
                withTimeout(timeoutMs) { result.await() }
            } catch (e: TimeoutCancellationException) {
                Log.error { "Timed out after ${timeoutMs}ms!" }
                // For some reason, if TimeoutCancellationException is thrown, runBlocking can
                // suspend indefinitely. Catch it and rethrow IllegalStateException.
                throw IllegalStateException("Timed out after ${timeoutMs}ms!")
            }
        }
    }

    /**
     * runBlockingWithTimeOrNull runs the specified [block] on a timeout of [timeoutMs] using the
     * given [dispatcher]. The function runs the given block asynchronously on a supervised scope,
     * allowing it to return after the timeout completes, even if the calling thread is blocked.
     * Returns null when the execution of the [block] times out.
     */
    fun <T> runBlockingCheckedOrNull(
        dispatcher: CoroutineDispatcher,
        timeoutMs: Long,
        block: suspend () -> T
    ): T? {
        return runBlocking {
            val result = runAsyncSupervised(dispatcher, block)
            withTimeoutOrNull(timeoutMs) { result.await() }
        }
    }

    private fun <T> runAsyncSupervised(
        dispatcher: CoroutineDispatcher,
        block: suspend () -> T
    ): Deferred<T> {
        return globalSupervisorScope.async(dispatcher) { block() }
    }
}
