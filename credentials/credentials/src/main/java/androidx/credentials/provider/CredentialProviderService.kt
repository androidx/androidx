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

import android.credentials.ClearCredentialStateException
import android.credentials.GetCredentialException
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.service.credentials.BeginCreateCredentialRequest
import android.service.credentials.BeginCreateCredentialResponse
import android.service.credentials.BeginGetCredentialRequest
import android.service.credentials.BeginGetCredentialResponse
import android.service.credentials.ClearCredentialStateRequest
import android.service.credentials.CredentialProviderService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.provider.utils.BeginCreateCredentialUtil
import androidx.credentials.provider.utils.BeginGetCredentialUtil

/**
 * Main service to be extended by provider services.
 *
 * Services that extend this class will be bound to by the framework, and receive
 * [androidx.credentials.CredentialManager] credential retrieval and creation API calls.
 */
@RequiresApi(34)
abstract class CredentialProviderService : CredentialProviderService() {

    /** @hide **/
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

    /** @hide **/
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

    final override fun onClearCredentialState(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void, ClearCredentialStateException>
    ) {
        val outcome = object : OutcomeReceiver<Void, ClearCredentialException> {
            override fun onResult(response: Void) {
                Log.i(
                    TAG, "onClearCredentialState result returned from provider to jetpack ")
                callback.onResult(response)
            }
            override fun onError(error: ClearCredentialException) {
                Log.i(
                    TAG, "onClearCredentialState result returned from provider to jetpack")
                super.onError(error)
                // TODO("Change error code to provider error when ready on framework")
                callback.onError(ClearCredentialStateException(error.type, error.message))
            }
        }
        onClearCredentialStateRequest(request, cancellationSignal, outcome)
    }

    /**
     * Called by the Credential Manager Jetpack library when the developer wishes to clear the
     * state of credentials.
     *
     * On this call, providers must clear previously stored state.
     * On completion, providers must call one of the [callback] methods to notify the result of the
     * request.
     *
     * @param [request] the [androidx.credentials.ClearCredentialStateRequest] to handle
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
     */
    abstract fun onClearCredentialStateRequest(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void,
            ClearCredentialException>
    )

    /**
     * Called by the Credential Manager Jetpack library to get credentials stored with a provider
     * service. Provider services must extend this in order to handle a
     * [ProviderGetCredentialRequest] request.
     *
     * Provider service must call one of the [callback] methods to notify the result of the
     * request.
     *
     * @param [request] the [ProviderGetCredentialRequest] to handle
     * See [BeginGetCredentialResponse] for the response to be returned
     * @param cancellationSignal signal for observing cancellation requests. The system will
     * use this to notify you that the result is no longer needed and you should stop
     * handling it in order to save your resources
     * @param callback the callback object to be used to notify the response or error
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
     */
    abstract fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse,
            CreateCredentialException>
    )

    /** @hide **/
    companion object {
        private const val TAG = "BaseService"
    }
}
