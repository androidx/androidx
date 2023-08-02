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
 * Returns a [Runnable] that will resume this [Continuation] when an operation completes and
 * the returned [Runnable]'s [Runnable.run] method is called.
 *
 * Useful for writing `suspend` bindings to async Jetpack library methods that accept [Runnable]
 * as a completion callback for a one-time operation:
 *
 * ```
 * public suspend fun FancinessManager.setFanciness(
 *     fanciness: Float
 * ): Unit = suspendCancellableCoroutine<Unit> { continuation ->
 *
 *     // Any Android API that supports cancellation should be configured to propagate
 *     // coroutine cancellation as follows:
 *     val canceller = CancellationSignal()
 *     continuation.invokeOnCancellation { canceller.cancel() }
 *
 *     // Invoke the FancinessManager#setFanciness method as follows:
 *     queryAsync(
 *         fanciness,
 *         canceller,
 *         // Use a direct executor to avoid extra dispatch. Resuming the continuation will
 *         // handle getting to the right thread or pool via the ContinuationInterceptor.
 *         Runnable::run,
 *         continuation.asRunnable()
 *     )
 * }
 * ```
 */
public fun Continuation<Unit>.asRunnable(): Runnable = ContinuationRunnable(this)

private class ContinuationRunnable(
    private val continuation: Continuation<Unit>
) : Runnable, AtomicBoolean(false) {
    override fun run() {
        // Do not attempt to resume more than once, even if the caller of the returned
        // Runnable is buggy and tries anyway.
        if (compareAndSet(false, true)) {
            continuation.resume(Unit)
        }
    }

    override fun toString() = "ContinuationRunnable(ran = ${get()})"
}
