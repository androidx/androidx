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

package androidx.credentials

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Manages user authentication flows.
 *
 * An application can call the CredentialManager apis to launch framework UI flows for a user to
 * register a new credential or to consent to a saved credential from supported credential
 * providers, which can then be used to authenticate to the app.
 */
@Suppress("UNUSED_PARAMETER")
class CredentialManager private constructor(private val context: Context) {
    companion object {
        @JvmStatic
        fun create(context: Context): CredentialManager = CredentialManager(context)
    }

    /**
     * Requests a credential from the user.
     *
     * The execution potentially launches framework UI flows for a user to view available
     * credentials, consent to using one of them, etc.
     *
     * Note: the [activity] parameter is no longer needed if your app targets minimum SDK version
     * 34 or above.
     *
     * @param request the request for getting the credential
     * @param activity the activity used to potentially launch any UI needed
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    // TODO(helenqin): support failure flow.
    suspend fun executeGetCredential(
        request: GetCredentialRequest,
        activity: Activity? = null,
    ): GetCredentialResponse = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val callback = object : CredentialManagerCallback<GetCredentialResponse> {
            override fun onResult(result: GetCredentialResponse) {
                continuation.resume(result)
            }

            override fun onError(e: CredentialManagerException) {
                continuation.resumeWithException(e)
            }
        }

        executeGetCredentialAsync(
            request,
            activity,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback)
    }

    /**
     * Registers a user credential that can be used to authenticate the user to
     * the app in the future.
     *
     * The execution potentially launches framework UI flows for a user to view their registration
     * options, grant consent, etc.
     *
     * Note: the [activity] parameter is no longer needed if your app targets minimum SDK version
     * 34 or above.
     *
     * @param request the request for creating the credential
     * @param activity the activity used to potentially launch any UI needed
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    suspend fun executeCreateCredential(
        request: CreateCredentialRequest,
        activity: Activity? = null,
    ): CreateCredentialResponse = suspendCancellableCoroutine { continuation ->
        // Any Android API that supports cancellation should be configured to propagate
        // coroutine cancellation as follows:
        val canceller = CancellationSignal()
        continuation.invokeOnCancellation { canceller.cancel() }

        val callback = object : CredentialManagerCallback<CreateCredentialResponse> {
            override fun onResult(result: CreateCredentialResponse) {
                continuation.resume(result)
            }

            override fun onError(e: CredentialManagerException) {
                continuation.resumeWithException(e)
            }
        }

        executeCreateCredentialAsync(
            request,
            activity,
            canceller,
            // Use a direct executor to avoid extra dispatch. Resuming the continuation will
            // handle getting to the right thread or pool via the ContinuationInterceptor.
            Runnable::run,
            callback)
    }

    /**
     * Java API for requesting a credential from the user.
     *
     * The execution potentially launches framework UI flows for a user to view available
     * credentials, consent to using one of them, etc.
     *
     * Note: the [activity] parameter is no longer needed if your app targets minimum SDK version
     * 34 or above.
     *
     * @param request the request for getting the credential
     * @param activity an optional activity used to potentially launch any UI needed
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    fun executeGetCredentialAsync(
        request: GetCredentialRequest,
        activity: Activity?,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse>,
    ) {
        throw UnsupportedOperationException("Unimplemented")
    }

    /**
     * Java API for registering a user credential that can be used to authenticate the user to
     * the app in the future.
     *
     * The execution potentially launches framework UI flows for a user to view their registration
     * options, grant consent, etc.
     *
     * Note: the [activity] parameter is no longer needed if your app targets minimum SDK version
     * 34 or above.
     *
     * @param request the request for creating the credential
     * @param activity an optional activity used to potentially launch any UI needed
     * @param cancellationSignal an optional signal that allows for cancelling this call
     * @param executor the callback will take place on this executor
     * @param callback the callback invoked when the request succeeds or fails
     * @throws UnsupportedOperationException Since the api is unimplemented
     */
    fun executeCreateCredentialAsync(
        request: CreateCredentialRequest,
        activity: Activity?,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse>,
    ) {
        throw UnsupportedOperationException("Unimplemented")
    }
}