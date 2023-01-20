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
import android.credentials.CredentialManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.internal.FrameworkImplHelper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Framework credential provider implementation that allows credential
 * manager requests to be routed to the framework.
 *
 * @hide
 */
@RequiresApi(34)
internal class CredentialProviderFrameworkImpl(context: Context) : CredentialProvider {
    private val credentialManager: CredentialManager =
        context.getSystemService(Context.CREDENTIAL_SERVICE) as CredentialManager

    override fun onGetCredential(
        request: GetCredentialRequest,
        activity: Activity,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
    ) {
        Log.i(TAG, "In CredentialProviderFrameworkImpl onGetCredential")

        val outcome = object : OutcomeReceiver<
            android.credentials.GetCredentialResponse, android.credentials.GetCredentialException> {
            override fun onResult(response: android.credentials.GetCredentialResponse) {
                Log.i(TAG, "GetCredentialResponse returned from framework")
                callback.onResult(convertGetResponseToJetpackClass(response))
            }

            override fun onError(error: android.credentials.GetCredentialException) {
                Log.i(TAG, "GetCredentialResponse error returned from framework")
                // TODO("Covert to the appropriate exception")
                callback.onError(GetCredentialUnknownException(error.message))
            }
        }
        credentialManager.getCredential(
            convertGetRequestToFrameworkClass(request),
            activity,
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            outcome
        )
    }

    override fun onCreateCredential(
        request: CreateCredentialRequest,
        activity: Activity,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>
    ) {
        Log.i(TAG, "In CredentialProviderFrameworkImpl onCreateCredential")

        val outcome = object : OutcomeReceiver<
            android.credentials.CreateCredentialResponse,
            android.credentials.CreateCredentialException> {
            override fun onResult(response: android.credentials.CreateCredentialResponse) {
                Log.i(TAG, "Create Result returned from framework: ")
                callback.onResult(
                    CreateCredentialResponse.createFrom(
                        request.type, response.data
                    )
                )
            }

            override fun onError(error: android.credentials.CreateCredentialException) {
                Log.i(TAG, "CreateCredentialResponse error returned from framework")
                // TODO("Covert to the appropriate exception")
                callback.onError(CreateCredentialUnknownException(error.message))
            }
        }

        credentialManager.createCredential(
            android.credentials.CreateCredentialRequest(
                request.type,
                FrameworkImplHelper.getFinalCreateCredentialData(request, activity),
                request.candidateQueryData,
                request.isSystemProviderRequired
            ),
            activity,
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            outcome
        )
    }

    private fun convertGetRequestToFrameworkClass(request: GetCredentialRequest):
        android.credentials.GetCredentialRequest {
        val builder = android.credentials.GetCredentialRequest.Builder(Bundle())
        request.credentialOptions.forEach {
            builder.addGetCredentialOption(
                android.credentials.GetCredentialOption(
                    it.type, it.requestData, it.candidateQueryData, it.isSystemProviderRequired
                )
            )
        }
        return builder.build()
    }

    internal fun convertGetResponseToJetpackClass(
        response: android.credentials.GetCredentialResponse
    ): GetCredentialResponse {
        val credential = response.credential
        return GetCredentialResponse(
            Credential.createFrom(
                credential.type, credential.data
            )
        )
    }

    override fun isAvailableOnDevice(): Boolean {
        // TODO("Base it on API level check")
        return true
    }

    override fun onClearCredential(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<Void?, ClearCredentialException>
    ) {
        // TODO("Not yet implemented")
    }

    /** @hide */
    companion object {
        private const val TAG = "CredManProvService"
    }
}