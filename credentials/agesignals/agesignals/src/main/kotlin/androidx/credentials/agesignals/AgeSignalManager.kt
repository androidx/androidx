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

package androidx.credentials.agesignals

import android.content.Context
import android.os.CancellationSignal
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Query the age signal of the current device user.
 *
 * Obtain an instance via [AgeSignalManager.create].
 */
public interface AgeSignalManager {

    public companion object {
        /**
         * Creates an [AgeSignalManager] instance associated with the given [context].
         *
         * @param context the context to associate with this manager
         * @return a concrete [AgeSignalManager]
         */
        @JvmStatic
        public fun create(context: Context): AgeSignalManager = AgeSignalManagerImpl(context)
    }

    /**
     * Requests the user's age range.
     *
     * The execution can potentially launch UI flows to collect user consent to sharing their age
     * range.
     *
     * @param context the [android.app.Activity] context used to launch any UI needed
     * @param request the request for getting the user's age range
     * @return the resolved [GetAgeRangeResponse]
     * @throws GetAgeRangeException if the request fails
     */
    public suspend fun getAgeRange(
        context: Context,
        request: GetAgeRangeRequest,
    ): GetAgeRangeResponse = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val callback =
            object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                override fun onResult(result: GetAgeRangeResponse) {
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onError(error: GetAgeRangeException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            }

        getAgeRangeAsync(
            context,
            request,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback,
        )
    }

    /**
     * Requests the user's age range.
     *
     * This API uses callbacks instead of Kotlin coroutines.
     *
     * The execution can potentially launch UI flows to collect user consent to sharing their age
     * range.
     *
     * @param context the [android.app.Activity] context used to launch any UI needed
     * @param request the request for getting the user's age range
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     */
    public fun getAgeRangeAsync(
        context: Context,
        request: GetAgeRangeRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    )
}
