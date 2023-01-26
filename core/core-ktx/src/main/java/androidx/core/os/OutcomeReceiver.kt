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

// OutcomeReceiver was added in API 31
@file:RequiresApi(31)
package androidx.core.os

import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Returns an [OutcomeReceiver] that will resume this [Continuation] when an outcome is reported.
 *
 * Useful for writing `suspend` bindings to async Android platform methods that accept
 * [OutcomeReceiver]:
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
 *         continuation.asOutcomeReceiver()
 *     )
 * }
 * ```
 */
@RequiresApi(31)
public fun <R, E : Throwable> Continuation<R>.asOutcomeReceiver(): OutcomeReceiver<R, E> =
    ContinuationOutcomeReceiver(this)

@RequiresApi(31)
private class ContinuationOutcomeReceiver<R, E : Throwable>(
    private val continuation: Continuation<R>
) : OutcomeReceiver<R, E>, AtomicBoolean(false) {
    override fun onResult(result: R & Any) {
        // Do not attempt to resume more than once, even if the caller of the returned
        // OutcomeReceiver is buggy and tries anyway.
        if (compareAndSet(false, true)) {
            continuation.resume(result)
        }
    }

    override fun onError(error: E) {
        // Do not attempt to resume more than once, even if the caller of the returned
        // OutcomeReceiver is buggy and tries anyway.
        if (compareAndSet(false, true)) {
            continuation.resumeWithException(error)
        }
    }

    override fun toString() = "ContinuationOutcomeReceiver(outcomeReceived = ${get()})"
}
