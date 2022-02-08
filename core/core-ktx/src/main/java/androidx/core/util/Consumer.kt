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

// java.util.function.Consumer was added in API 24
@file:RequiresApi(24)
package androidx.core.util

import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Returns a [java.util.function.Consumer] that will resume this [Continuation] when the result of
 * an operation is [accepted][java.util.function.Consumer.accept].
 *
 * Useful for writing `suspend` bindings to async methods that accept
 * [java.util.function.Consumer] as a result callback for a one-time operation:
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
 *         continuation.asConsumer()
 *     )
 * }
 * ```
 */
@RequiresApi(24)
public fun <T> Continuation<T>.asConsumer(): java.util.function.Consumer<T> =
    ContinuationConsumer(this)

@RequiresApi(24)
private class ContinuationConsumer<T>(
    private val continuation: Continuation<T>
) : java.util.function.Consumer<T>, AtomicBoolean(false) {
    override fun accept(value: T) {
        // Do not attempt to resume more than once, even if the caller of the returned
        // Consumer is buggy and tries anyway.
        if (compareAndSet(false, true)) {
            continuation.resume(value)
        }
    }

    override fun toString() = "ContinuationConsumer(resultAccepted = ${get()})"
}
