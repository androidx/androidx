/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.playservices.controllers.identitycredentials.createdigitalcredential

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.CreateDigitalCredentialRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderController
import androidx.credentials.playservices.controllers.identitycredentials.IdentityCredentialApiHiddenActivity
import androidx.credentials.provider.PendingIntentHandler
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.CreateCredentialResponse
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import java.util.concurrent.Executor

/** A controller to handle the CreateCredential flow with play services. */
@OptIn(ExperimentalDigitalCredentialApi::class)
@RequiresApi(23)
internal class CreateDigitalCredentialController(private val context: Context) :
    CredentialProviderController<
        CreateDigitalCredentialRequest,
        CreateCredentialRequest,
        CreateCredentialResponse,
        androidx.credentials.CreateCredentialResponse,
        CreateCredentialException,
    >(context) {

    /** The callback object state, used in the protected handleResponse method. */
    @VisibleForTesting
    lateinit var callback:
        CredentialManagerCallback<
            androidx.credentials.CreateCredentialResponse,
            CreateCredentialException,
        >

    /** The callback requires an executor to invoke it. */
    @VisibleForTesting lateinit var executor: Executor

    /**
     * The cancellation signal. Which is shuttled around to stop the flow at any moment prior to
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
                            createCredentialExceptionTypeToException,
                        executor,
                        callback,
                        cancellationSignal,
                    )
                ) {
                    return
                } else {
                    handleResponse(
                        resultData.getInt(ACTIVITY_REQUEST_CODE_TAG),
                        resultCode,
                        resultData.getParcelable(RESULT_DATA_TAG),
                    )
                }
            }
        }

    internal fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        if (uniqueRequestCode != CONTROLLER_REQUEST_CODE) {
            Log.w(
                TAG,
                "Returned request code $CONTROLLER_REQUEST_CODE which " +
                    " does not match what was given $uniqueRequestCode",
            )
            return
        }
        if (
            maybeReportErrorResultCodeCreate(
                resultCode,
                { s, f -> cancelOrCallbackExceptionOrResult(s, f) },
                { e -> this.executor.execute { this.callback.onError(e) } },
                cancellationSignal,
            )
        ) {
            return
        }

        if (data == null) {
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                this.executor.execute {
                    this.callback.onError(
                        CreateCredentialUnknownException("No provider data returned.")
                    )
                }
            }
        } else {
            val response =
                PendingIntentHandler.retrieveCreateCredentialResponse(
                    type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
                    intent = data,
                )
            if (response == null) {
                val providerException = PendingIntentHandler.retrieveCreateCredentialException(data)
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    this.executor.execute {
                        this.callback.onError(
                            providerException
                                ?: CreateCredentialUnknownException(
                                    "Unexpected configuration error"
                                )
                        )
                    }
                }
            } else {
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    this.executor.execute { this.callback.onResult(response) }
                }
            }
        }
    }

    override fun invokePlayServices(
        request: CreateDigitalCredentialRequest,
        callback:
            CredentialManagerCallback<
                androidx.credentials.CreateCredentialResponse,
                CreateCredentialException,
            >,
        executor: Executor,
        cancellationSignal: CancellationSignal?,
    ) {
        this.cancellationSignal = cancellationSignal
        this.callback = callback
        this.executor = executor
        if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
            return
        }

        val convertedRequest = this.convertRequestToPlayServices(request)
        IdentityCredentialManager.getClient(context)
            .createCredential(convertedRequest)
            .addOnSuccessListener { result ->
                if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
                    return@addOnSuccessListener
                }
                val hiddenIntent = Intent(context, IdentityCredentialApiHiddenActivity::class.java)
                hiddenIntent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                hiddenIntent.putExtra(
                    RESULT_RECEIVER_TAG,
                    toIpcFriendlyResultReceiver(resultReceiver),
                )
                hiddenIntent.putExtra(EXTRA_FLOW_PENDING_INTENT, result.pendingIntent)
                hiddenIntent.putExtra(EXTRA_ERROR_NAME, CREATE_UNKNOWN)
                context.startActivity(hiddenIntent)
            }
            .addOnFailureListener { e ->
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    executor.execute {
                        callback.onError(CreateCredentialUnknownException(e.message))
                    }
                }
            }
    }

    public override fun convertRequestToPlayServices(
        request: CreateDigitalCredentialRequest
    ): CreateCredentialRequest {
        return CreateCredentialRequest(
            type = request.type,
            credentialData = request.credentialData,
            candidateQueryData = request.candidateQueryData,
            origin = request.origin,
            requestJson = request.requestJson,
            resultReceiver = resultReceiver,
        )
    }

    public override fun convertResponseToCredentialManager(
        response: CreateCredentialResponse
    ): androidx.credentials.CreateCredentialResponse {
        return androidx.credentials.CreateCredentialResponse.createFrom(
            type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
            data = response.data,
        )
    }

    private companion object {
        private const val TAG = "DigitalCredentialClient"
    }
}
