/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.internal

import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.core.Token
import androidx.camera.camera2.pipe.core.acquireToken
import androidx.camera.camera2.pipe.core.acquireTokenAndSuspend
import androidx.camera.camera2.pipe.core.tryAcquireToken
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex

/**
 * Manages mutual exclusion for camera graph operations using a token-based system.
 *
 * This class ensures that actions are executed sequentially and provides utilities to run code
 * within a specific [CoroutineScope] while holding a session lock.
 */
@CameraGraphScope
internal class GraphSessionLock @Inject constructor() {
    private val mutex = Mutex()

    internal suspend fun acquireToken(): Token = mutex.acquireToken()

    internal fun tryAcquireToken(): Token? = mutex.tryAcquireToken()

    internal fun <T> withTokenIn(
        scope: CoroutineScope,
        action: suspend (token: Token) -> T,
    ): Deferred<T> =
        asyncUndispatched(scope) {
            // Note: It's _very_ important to suspend here (which acquireTokenAndSuspend does) to
            // ensure the action occurs on the correct scope thread.
            mutex.acquireTokenAndSuspend().use { token -> action(token) }
        }

    internal fun <T> withTokenInAsync(
        scope: CoroutineScope,
        action: suspend (token: Token) -> Deferred<T>,
    ): Deferred<T> =
        asyncUndispatched(scope) {
            // Note: It's _very_ important to suspend here (which acquireTokenAndSuspend does) to
            // ensure the action occurs on the correct scope thread.
            val deferred = mutex.acquireTokenAndSuspend().use { token -> action(token) }

            ensureActive()
            deferred.await()
        }

    private fun <T> asyncUndispatched(
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> T,
    ): Deferred<T> {
        // https://github.com/Kotlin/kotlinx.coroutines/issues/1578
        // To handle `runBlocking` we need to use `job.complete()` in `result.invokeOnCompletion`.
        // However, if we do this directly on the scope that is provided it will cause
        // SupervisorScopes to block and never complete. To work around this, we create a childJob,
        // propagate the existing context, and use that as the context for scope.async.
        val childJob = Job(scope.coroutineContext[Job.Key])
        val context = scope.coroutineContext + childJob
        val result =
            scope.async(context = context, start = CoroutineStart.UNDISPATCHED) {
                ensureActive() // Exit early if the parent scope has been canceled.

                // It is very important to acquire *and* suspend here. Invoking a coroutine using
                // UNDISPATCHED will execute on the current thread until the suspension point, and
                // this will force the execution to switch to the provided scope after ensuring the
                // lock is acquired or in the queue. This guarantees exclusion, ordering, and
                // execution within the correct scope.
                block()
            }
        result.invokeOnCompletion { childJob.complete() }
        return result
    }

    private suspend fun <T> Token.use(block: suspend (Token) -> T): T {
        try {
            return block(this)
        } finally {
            this.release()
        }
    }
}
