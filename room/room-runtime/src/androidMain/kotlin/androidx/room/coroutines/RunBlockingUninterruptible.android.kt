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

package androidx.room.coroutines

import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * An alternative version of [runBlocking] that is not interruptible. Used to bridge between Room's
 * existing blocking APIs, including blocking DAO functions and its coroutines internals such as the
 * connection pool and invalidation tracker.
 *
 * This function is necessary to maintain backwards compatibility with existing blocking APIs
 * behaviour as they had never been interruptible and do not throw [InterruptedException].
 *
 * See also b/400584611 and https://github.com/Kotlin/kotlinx.coroutines/issues/4384
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
internal fun <T> runBlockingUninterruptible(block: suspend CoroutineScope.() -> T): T {
    // Preemptively clear the interrupt flag if any, otherwise `runBlocking` will throw.
    Thread.interrupted()
    // This outer `runBlocking` does not suspend, its body will always be running until completion.
    return runBlocking outer@{
        // Get the internal event loop dispatcher created by `runBlocking` so that the global
        // coroutine launched uses the same thread as the one being blocked avoiding a thread hop
        // and preserving thread locals.
        val dispatcher = coroutineContext[ContinuationInterceptor]!!
        val deferred = CompletableDeferred<T>()
        GlobalScope.launch(dispatcher, CoroutineStart.UNDISPATCHED) {
            deferred.completeWith(runCatching { block() })
        }
        // Repeatedly run blockly a coroutine that awaits for the `deferred` catching and
        // ignoring interruptions until the global coroutine finishes running the `block`.
        while (!deferred.isCompleted) {
            try {
                return@outer runBlocking(dispatcher) { deferred.await() }
            } catch (_: InterruptedException) {
                // We got interrupted or `await()` completed with an `InterruptedException`
            }
        }
        // The `deferred` has completed (probably with an InterruptedException), return the result.
        deferred.getCompleted()
    }
}
