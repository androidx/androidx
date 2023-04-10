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

import android.annotation.SuppressLint
import android.content.Context
import android.credentials.CredentialManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialUnknownException
import androidx.credentials.exceptions.ClearCredentialUnsupportedException
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
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
class CredentialProviderFrameworkImpl(context: Context) : CredentialProvider {
    private val credentialManager: CredentialManager? =
        context.getSystemService(Context.CREDENTIAL_SERVICE) as CredentialManager?

    override fun onPrepareCredential(
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<PrepareGetCredentialResponse, GetCredentialException>
    ) {
        if (isCredmanDisabled {
                callback.onError(
                    GetCredentialUnsupportedException(
                        "Your device doesn't support credential manager"
                    )
                )
            }) return
        val outcome = object : OutcomeReceiver<
            android.credentials.PrepareGetCredentialResponse,
            android.credentials.GetCredentialException> {
            override fun onResult(response: android.credentials.PrepareGetCredentialResponse) {
                callback.onResult(convertPrepareGetResponseToJetpackClass(response))
            }

            override fun onError(error: android.credentials.GetCredentialException) {
                callback.onError(convertToJetpackGetException(error))
            }
        }

        credentialManager!!.prepareGetCredential(
            convertGetRequestToFrameworkClass(request),
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            outcome
        )
    }

    override fun onGetCredential(
        context: Context,
        pendingGetCredentialHandle: PrepareGetCredentialResponse.PendingGetCredentialHandle,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
    ) {
        if (isCredmanDisabled {
                callback.onError(
                    GetCredentialUnsupportedException(
                        "Your device doesn't support credential manager"
                    )
                )
            }) return
        val outcome = object : OutcomeReceiver<
            android.credentials.GetCredentialResponse, android.credentials.GetCredentialException> {
            override fun onResult(response: android.credentials.GetCredentialResponse) {
                callback.onResult(convertGetResponseToJetpackClass(response))
            }

            override fun onError(error: android.credentials.GetCredentialException) {
                callback.onError(convertToJetpackGetException(error))
            }
        }

        credentialManager!!.getCredential(
            context,
            pendingGetCredentialHandle.frameworkHandle!!,
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            outcome
        )
    }

    override fun onGetCredential(
        context: Context,
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
    ) {
        if (isCredmanDisabled {
                callback.onError(
                    GetCredentialUnsupportedException(
                        "Your device doesn't support credential manager"
                    )
                )
            }) return

        val outcome = object : OutcomeReceiver<
            android.credentials.GetCredentialResponse, android.credentials.GetCredentialException> {
            override fun onResult(response: android.credentials.GetCredentialResponse) {
                Log.i(TAG, "GetCredentialResponse returned from framework")
                callback.onResult(convertGetResponseToJetpackClass(response))
            }

            override fun onError(error: android.credentials.GetCredentialException) {
                Log.i(TAG, "GetCredentialResponse error returned from framework")
                callback.onError(convertToJetpackGetException(error))
            }
        }

        credentialManager!!.getCredential(
            context,
            convertGetRequestToFrameworkClass(request),
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            outcome
        )
    }

    private fun isCredmanDisabled(handleNullCredMan: () -> Unit): Boolean {
        if (credentialManager == null) {
            handleNullCredMan()
            return true
        }
        return false
    }

    override fun onCreateCredential(
        context: Context,
        request: CreateCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>
    ) {
        if (isCredmanDisabled {
                callback.onError(
                    CreateCredentialUnsupportedException(
                        "Your device doesn't support credential manager"
                    )
                )
            }) return
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
                callback.onError(convertToJetpackCreateException(error))
            }
        }

        credentialManager!!.createCredential(
            context,
            convertCreateRequestToFrameworkClass(request, context),
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            outcome
        )
    }

    private fun convertCreateRequestToFrameworkClass(
        request: CreateCredentialRequest,
        context: Context
    ): android.credentials.CreateCredentialRequest {
        val createCredentialRequestBuilder: android.credentials.CreateCredentialRequest.Builder =
            android.credentials.CreateCredentialRequest
                .Builder(request.type,
                    FrameworkImplHelper.getFinalCreateCredentialData(request, context),
                    request.candidateQueryData)
                .setIsSystemProviderRequired(request.isSystemProviderRequired)
                // TODO("change to taking value from the request when ready")
                .setAlwaysSendAppInfoToProvider(true)
        setOriginForCreateRequest(request, createCredentialRequestBuilder)
        return createCredentialRequestBuilder.build()
    }

    @SuppressLint("MissingPermission")
    private fun setOriginForCreateRequest(
        request: CreateCredentialRequest,
        builder: android.credentials.CreateCredentialRequest.Builder
    ) {
        if (request.origin != null) {
            builder.setOrigin(request.origin)
        }
    }

    private fun convertGetRequestToFrameworkClass(request: GetCredentialRequest):
        android.credentials.GetCredentialRequest {
        val builder = android.credentials.GetCredentialRequest.Builder(
            GetCredentialRequest.toRequestDataBundle(request))
        request.credentialOptions.forEach {
            builder.addCredentialOption(
                android.credentials.CredentialOption.Builder(
                    it.type, it.requestData, it.candidateQueryData
                ).setIsSystemProviderRequired(
                    it.isSystemProviderRequired
                ).setAllowedProviders(it.allowedProviders).build()
            )
        }
        setOriginForGetRequest(request, builder)
        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun setOriginForGetRequest(
        request: GetCredentialRequest,
        builder: android.credentials.GetCredentialRequest.Builder
    ) {
        if (request.origin != null) {
            builder.setOrigin(request.origin)
        }
    }

    private fun createFrameworkClearCredentialRequest():
        android.credentials.ClearCredentialStateRequest {
        return android.credentials.ClearCredentialStateRequest(Bundle())
    }

    internal fun convertToJetpackGetException(error: android.credentials.GetCredentialException):
        GetCredentialException {
        return when (error.type) {
            android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL ->
                NoCredentialException(error.message)

            android.credentials.GetCredentialException.TYPE_USER_CANCELED ->
                GetCredentialCancellationException(error.message)

            android.credentials.GetCredentialException.TYPE_INTERRUPTED ->
                GetCredentialInterruptedException(error.message)

            else -> GetCredentialUnknownException(error.message)
        }
    }

    internal fun convertToJetpackCreateException(
        error: android.credentials.CreateCredentialException
    ): CreateCredentialException {
        return when (error.type) {
            android.credentials.CreateCredentialException.TYPE_NO_CREATE_OPTIONS ->
                CreateCredentialNoCreateOptionException(error.message)

            android.credentials.CreateCredentialException.TYPE_USER_CANCELED ->
                CreateCredentialCancellationException(error.message)

            android.credentials.CreateCredentialException.TYPE_INTERRUPTED ->
                CreateCredentialInterruptedException(error.message)

            else -> CreateCredentialUnknownException(error.message)
        }
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

    internal fun convertPrepareGetResponseToJetpackClass(
        response: android.credentials.PrepareGetCredentialResponse
    ): PrepareGetCredentialResponse {
        return PrepareGetCredentialResponse(
            response,
            PrepareGetCredentialResponse.PendingGetCredentialHandle(
                response.pendingGetCredentialHandle,
            )
        )
    }

    override fun isAvailableOnDevice(): Boolean {
        // TODO("b/276492529 Base it on API level check")
        return true
    }

    override fun onClearCredential(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<Void?, ClearCredentialException>
    ) {
        Log.i(TAG, "In CredentialProviderFrameworkImpl onClearCredential")

        if (isCredmanDisabled { ->
                callback.onError(
                    ClearCredentialUnsupportedException(
                        "Your device doesn't support credential manager"
                    )
                )
            }) return

        val outcome = object : OutcomeReceiver<Void,
            android.credentials.ClearCredentialStateException> {
            override fun onResult(response: Void) {
                Log.i(TAG, "Clear result returned from framework: ")
                callback.onResult(response)
            }

            override fun onError(error: android.credentials.ClearCredentialStateException) {
                Log.i(TAG, "ClearCredentialStateException error returned from framework")
                // TODO("Covert to the appropriate exception")
                callback.onError(ClearCredentialUnknownException())
            }
        }

        credentialManager!!.clearCredentialState(
            createFrameworkClearCredentialRequest(),
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            outcome
        )
    }

    /** @hide */
    companion object {
        private const val TAG = "CredManProvService"
    }
}