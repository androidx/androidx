/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.playservices.controllers.GetRestoreCredential

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.credentials.Credential
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.DigitalCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.internal.toJetpackGetException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.IdentityCredentialApiHiddenActivity
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import com.google.android.gms.identitycredentials.IntentHelper
import java.util.concurrent.Executor

/** A controller to handle the GetRestoreCredential flow with play services. */
internal class CredentialProviderGetDigitalCredentialController(private val context: Context) :
    CredentialProviderController<
        GetCredentialRequest,
        com.google.android.gms.identitycredentials.GetCredentialRequest,
        com.google.android.gms.identitycredentials.GetCredentialResponse,
        GetCredentialResponse,
        GetCredentialException
    >(context) {

    /** The callback object state, used in the protected handleResponse method. */
    @VisibleForTesting
    lateinit var callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>

    /** The callback requires an executor to invoke it. */
    @VisibleForTesting lateinit var executor: Executor

    /**
     * The cancellation signal, which is shuttled around to stop the flow at any moment prior to
     * returning data.
     */
    @VisibleForTesting private var cancellationSignal: CancellationSignal? = null

    @Suppress("deprecation")
    private val resultReceiver =
        object : ResultReceiver(Handler(Looper.getMainLooper())) {
            public override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (
                    maybeReportErrorFromResultReceiver(
                        resultData,
                        CredentialProviderBaseController.Companion::
                            getCredentialExceptionTypeToException,
                        executor,
                        callback,
                        cancellationSignal
                    )
                ) {
                    return
                } else {
                    handleResponse(
                        resultData.getInt(ACTIVITY_REQUEST_CODE_TAG),
                        resultCode,
                        resultData.getParcelable(RESULT_DATA_TAG)
                    )
                }
            }
        }

    internal fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        if (uniqueRequestCode != CONTROLLER_REQUEST_CODE) {
            Log.w(
                TAG,
                "Returned request code $CONTROLLER_REQUEST_CODE which " +
                    " does not match what was given $uniqueRequestCode"
            )
            return
        }

        if (
            maybeReportErrorResultCodeGet(
                resultCode,
                { s, f -> cancelOrCallbackExceptionOrResult(s, f) },
                { e -> this.executor.execute { this.callback.onError(e) } },
                cancellationSignal
            )
        ) {
            return
        }

        try {
            val response = IntentHelper.extractGetCredentialResponse(resultCode, data?.extras!!)
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                this.executor.execute {
                    this.callback.onResult(convertResponseToCredentialManager(response))
                }
            }
        } catch (e: Exception) {
            val getException = fromGmsException(e)
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                executor.execute { callback.onError(getException) }
            }
        }
    }

    override fun invokePlayServices(
        request: GetCredentialRequest,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
        executor: Executor,
        cancellationSignal: CancellationSignal?
    ) {
        this.cancellationSignal = cancellationSignal
        this.callback = callback
        this.executor = executor

        if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
            return
        }

        val convertedRequest = this.convertRequestToPlayServices(request)
        IdentityCredentialManager.getClient(context)
            .getCredential(convertedRequest)
            .addOnSuccessListener { result ->
                if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
                    return@addOnSuccessListener
                }
                val hiddenIntent = Intent(context, IdentityCredentialApiHiddenActivity::class.java)
                hiddenIntent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                hiddenIntent.putExtra(
                    RESULT_RECEIVER_TAG,
                    toIpcFriendlyResultReceiver(resultReceiver)
                )
                hiddenIntent.putExtra(EXTRA_GET_CREDENTIAL_INTENT, result.pendingIntent)
                context.startActivity(hiddenIntent)
            }
            .addOnFailureListener { e ->
                val getException = fromGmsException(e)
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    executor.execute { callback.onError(getException) }
                }
            }
    }

    private fun fromGmsException(e: Throwable): GetCredentialException {
        return when (e) {
            is com.google.android.gms.identitycredentials.GetCredentialException ->
                toJetpackGetException(e.type, e.message)
            is ApiException ->
                when (e.statusCode) {
                    CommonStatusCodes.CANCELED -> {
                        GetCredentialCancellationException(e.message)
                    }
                    in retryables -> {
                        GetCredentialInterruptedException(e.message)
                    }
                    else -> {
                        GetCredentialUnknownException("Get digital credential failed, failure: $e")
                    }
                }
            else -> GetCredentialUnknownException("Get digital credential failed, failure: $e")
        }
    }

    public override fun convertRequestToPlayServices(
        request: GetCredentialRequest
    ): com.google.android.gms.identitycredentials.GetCredentialRequest {
        val credOptions =
            mutableListOf<com.google.android.gms.identitycredentials.CredentialOption>()
        for (option in request.credentialOptions) {
            if (option is GetDigitalCredentialOption) {
                credOptions.add(
                    com.google.android.gms.identitycredentials.CredentialOption(
                        option.type,
                        option.requestData,
                        option.candidateQueryData,
                        option.requestJson,
                        requestType = "",
                        protocolType = "",
                    )
                )
            }
        }
        return com.google.android.gms.identitycredentials.GetCredentialRequest(
            credOptions,
            GetCredentialRequest.getRequestMetadataBundle(request),
            request.origin,
            ResultReceiver(null) // No-op
        )
    }

    public override fun convertResponseToCredentialManager(
        response: com.google.android.gms.identitycredentials.GetCredentialResponse
    ): GetCredentialResponse {
        return GetCredentialResponse(
            Credential.createFrom(
                DigitalCredential.TYPE_DIGITAL_CREDENTIAL, // TODO: b/361100869 - use the real type
                // returned as the response
                response.credential.data,
            )
        )
    }

    private companion object {
        private const val TAG = "DigitalCredentialClient"
    }
}
