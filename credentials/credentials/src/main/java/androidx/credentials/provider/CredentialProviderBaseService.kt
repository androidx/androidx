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

package androidx.credentials.provider

import android.credentials.GetCredentialException
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.service.credentials.BeginCreateCredentialRequest
import android.service.credentials.BeginCreateCredentialResponse
import android.service.credentials.BeginGetCredentialRequest
import android.service.credentials.BeginGetCredentialResponse
import android.service.credentials.CredentialProviderService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.provider.utils.BeginCreateCredentialUtil
import androidx.credentials.provider.utils.BeginGetCredentialUtil

/**
 * Credential Provider base service to be extended by provider services.
 *
 * This class extends from the framework [CredentialProviderService], and is
 * called by the framework on credential get and create requests. The framework
 * requests are converted to structured jetpack requests, and sent to
 * provider services that extend from this service.
 *
 * @hide
 */
@RequiresApi(34)
abstract class CredentialProviderBaseService : CredentialProviderService() {
    final override fun onBeginGetCredential(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        val structuredRequest = BeginGetCredentialUtil.convertToStructuredRequest(request)
        val outcome = object : OutcomeReceiver<BeginGetCredentialResponse,
            androidx.credentials.exceptions.GetCredentialException> {
            override fun onResult(response: BeginGetCredentialResponse?) {
                Log.i(TAG, "onGetCredentials response returned from provider " +
                    "to jetpack library")
                callback.onResult(response)
            }

            override fun onError(error: androidx.credentials.exceptions.GetCredentialException) {
                super.onError(error)
                Log.i(TAG, "onGetCredentials error returned from provider " +
                    "to jetpack library")
                // TODO("Change error code to provider error when ready on framework")
                callback.onError(GetCredentialException(error.type, error.message))
            }
        }
        this.onBeginGetCredentialRequest(structuredRequest, cancellationSignal, outcome)
    }

    final override fun onBeginCreateCredential(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse,
            android.credentials.CreateCredentialException>
    ) {
        val outcome = object : OutcomeReceiver<
            BeginCreateCredentialResponse, CreateCredentialException> {
            override fun onResult(response: BeginCreateCredentialResponse?) {
                Log.i(
                    TAG, "onCreateCredential result returned from provider to jetpack " +
                        "library with credential entries size: " + response?.createEntries?.size)
                callback.onResult(response)
            }
            override fun onError(error: CreateCredentialException) {
                Log.i(
                    TAG, "onCreateCredential result returned from provider to jetpack")
                super.onError(error)
                // TODO("Change error code to provider error when ready on framework")
                callback.onError(android.credentials.CreateCredentialException(
                    error.type, error.message))
            }
        }
        onBeginCreateCredentialRequest(
            BeginCreateCredentialUtil.convertToStructuredRequest(request),
            cancellationSignal, outcome)
    }

    /**
     * Called by the Credential Manager Jetpack library to get credentials stored with a provider
     * service. Provider services must extend this in order to handle a
     * [GetCredentialProviderRequest] request.
     *
     * Provider service must call one of the [callback] methods to notify the result of the
     * request.
     *
     * @param [request] the [GetCredentialProviderRequest] to handle
     * See [BeginGetCredentialResponse] for the response to be returned
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     *
     * @hide
     */
    abstract fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse,
            androidx.credentials.exceptions.GetCredentialException>
    )

    /**
     * Called by the Credential Manager Jetpack library to begin a credential registration flow
     * with a credential provider service. Provider services must extend this in order to handle a
     * [BeginCreateCredentialRequest] request.
     *
     * Provider service must call one of the [callback] methods to notify the result of the
     * request.
     *
     * @param [request] the [BeginCreateCredentialRequest] to handle
     * See [BeginCreateCredentialResponse] for the response to be returned
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     *
     * @hide
     */
    abstract fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse,
            CreateCredentialException>
    )

    companion object {
        private const val TAG = "BaseService"
    }
}
