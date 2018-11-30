/*
 * Copyright 2018 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.work

import android.support.annotation.RestrictTo
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits for the completion of the [ListenableFuture] without blocking a thread.
 *
 * @return R The result from the [ListenableFuture]
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
suspend inline fun <R> ListenableFuture<R>.await(): R {
    // Fast path
    if (isDone) {
        try {
            return get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }
    return suspendCancellableCoroutine { cancellableContinuation ->
        addListener(Runnable {
            try {
                cancellableContinuation.resume(get())
            } catch (throwable: Throwable) {
                val cause = throwable.cause ?: throwable
                when (throwable) {
                    is CancellationException -> cancellableContinuation.cancel(cause)
                    else -> cancellableContinuation.resumeWithException(cause)
                }
            }
        }, DirectExecutor.INSTANCE)
    }
}
