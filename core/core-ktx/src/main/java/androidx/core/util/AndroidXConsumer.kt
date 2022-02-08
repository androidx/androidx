/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.core.util

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Returns a [Consumer] that will resume this [Continuation] when the result of an operation
 * is [accepted][Consumer.accept].
 *
 * Useful for writing `suspend` bindings to async Jetpack library methods that accept [Consumer]
 * as a result callback for a one-time operation:
 *
 * ```
 * public suspend fun FancinessManager.query(
 *     query: FancinessManager.Query
 * ): FancinessManager.QueryResult = suspendCancellableCoroutine<QueryResult> { continuation ->
 *
 *     // Any Android API that supports cancellation should be configured to propagate
 *     // coroutine cancellation as follows:
 *     val canceller = CancellationSignal()
 *     continuation.invokeOnCancellation { canceller.cancel() }
 *
 *     // Invoke the FancinessManager#queryAsync method as follows:
 *     queryAsync(
 *         query,
 *         canceller,
 *         // Use a direct executor to avoid extra dispatch. Resuming the continuation will
 *         // handle getting to the right thread or pool via the ContinuationInterceptor.
 *         Runnable::run,
 *         continuation.asAndroidXConsumer()
 *     )
 * }
 * ```
 */
public fun <T> Continuation<T>.asAndroidXConsumer(): Consumer<T> =
    AndroidXContinuationConsumer(this)

private class AndroidXContinuationConsumer<T>(
    private val continuation: Continuation<T>
) : Consumer<T>, AtomicBoolean(false) {
    override fun accept(value: T) {
        // Do not attempt to resume more than once, even if the caller of the returned
        // Consumer is buggy and tries anyway.
        if (compareAndSet(false, true)) {
            continuation.resume(value)
        }
    }

    override fun toString() = "ContinuationConsumer(resultAccepted = ${get()})"
}
