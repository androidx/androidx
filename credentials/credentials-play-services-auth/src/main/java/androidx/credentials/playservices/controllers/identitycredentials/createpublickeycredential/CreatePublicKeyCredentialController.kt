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

package androidx.credentials.playservices.controllers.identitycredentials.createpublickeycredential

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.os.BundleCompat.getParcelable
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderController
import androidx.credentials.playservices.controllers.identityauth.HiddenActivity
import androidx.credentials.provider.PendingIntentHandler
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.UnsupportedApiCallException
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.CreateCredentialResponse
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import java.util.concurrent.Executor

/** A controller to handle the CreateCredential flow with play services. */
@RequiresApi(Build.VERSION_CODES.M)
internal class CreatePublicKeyCredentialController(private val context: Context) :
    CredentialProviderController<
        CreatePublicKeyCredentialRequest,
        CreateCredentialRequest,
        CreateCredentialResponse,
        androidx.credentials.CreateCredentialResponse,
        CreateCredentialException,
    >(context) {

    /** The callback object state, used in the protected handleResponse method. */
    @VisibleForTesting
    private lateinit var callback:
        CredentialManagerCallback<
            androidx.credentials.CreateCredentialResponse,
            CreateCredentialException,
        >

    /** The callback requires an executor to invoke it. */
    @VisibleForTesting private lateinit var executor: Executor

    /**
     * The cancellation signal, which is shuttled around to stop the flow at any moment prior to
     * returning data.
     */
    @VisibleForTesting private var cancellationSignal: CancellationSignal? = null
    private val resultReceiver =
        object : ResultReceiver(Handler(Looper.getMainLooper())) {
            public override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (
                    maybeReportErrorFromResultReceiver(
                        resultData,
                        CredentialProviderBaseController.Companion::
                            createCredentialExceptionTypeToException,
                        executor = executor,
                        callback = callback,
                        cancellationSignal,
                    )
                )
                    return
                handleResponse(
                    resultData.getInt(ACTIVITY_REQUEST_CODE_TAG),
                    resultCode,
                    getParcelable(resultData, RESULT_DATA_TAG, Intent::class.java),
                )
            }
        }

    override fun invokePlayServices(
        request: CreatePublicKeyCredentialRequest,
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
        if (CredentialProviderPlayServicesImpl.Companion.cancellationReviewer(cancellationSignal)) {
            return
        }

        val convertedRequest = this.convertRequestToPlayServices(request)
        IdentityCredentialManager.Companion.getClient(context)
            .createCredential(convertedRequest)
            .addOnSuccessListener {
                val pendingIntent = it.pendingIntent
                val createCredentialResponse: CreateCredentialResponse? =
                    it.createCredentialResponse
                if (pendingIntent == null && createCredentialResponse == null) {
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute { callback.onError(CreateCredentialUnknownException()) }
                    }
                    return@addOnSuccessListener
                }
                if (pendingIntent != null) {
                    val hiddenIntent = Intent(context, HiddenActivity::class.java)
                    generateHiddenActivityIntent(
                        resultReceiver,
                        hiddenIntent,
                        CREATE_PUBLIC_KEY_CREDENTIAL_TAG,
                    )
                    hiddenIntent.putExtra(EXTRA_FLOW_PENDING_INTENT, pendingIntent)
                    try {
                        context.startActivity(hiddenIntent)
                    } catch (_: Exception) {
                        cancelOrCallbackExceptionOrResult(cancellationSignal) {
                            this.executor.execute {
                                this.callback.onError(
                                    CreateCredentialUnknownException(
                                        ERROR_MESSAGE_START_ACTIVITY_FAILED
                                    )
                                )
                            }
                        }
                    }
                }
                if (createCredentialResponse != null) {
                    val response = this.convertResponseToCredentialManager(createCredentialResponse)
                    if (response is CreatePublicKeyCredentialResponse) {
                        cancelOrCallbackExceptionOrResult(cancellationSignal) {
                            executor.execute { callback.onResult(result = response) }
                        }
                        return@addOnSuccessListener
                    }
                }
                if (pendingIntent == null) {
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute { callback.onError(CreateCredentialUnknownException()) }
                    }
                }
            }
            .addOnFailureListener { e ->
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    val exception = fromGmsException(e)
                    executor.execute { callback.onError(exception) }
                }
            }
    }

    internal fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        if (uniqueRequestCode != CONTROLLER_REQUEST_CODE) {
            Log.w(
                TAG,
                "Returned request code " +
                    "$CONTROLLER_REQUEST_CODE does not match what was given $uniqueRequestCode",
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
                executor.execute {
                    callback.onError(CreateCredentialUnknownException("No provider data returned."))
                }
            }
        } else {
            val response =
                PendingIntentHandler.retrieveCreateCredentialResponse(
                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
                    data,
                )
            if (response != null) {
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    executor.execute { callback.onResult(response) }
                }
            } else {
                val providerException = PendingIntentHandler.retrieveCreateCredentialException(data)
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    executor.execute {
                        callback.onError(
                            providerException
                                ?: CreateCredentialUnknownException("No provider data returned")
                        )
                    }
                }
            }
        }
    }

    fun fromGmsException(e: Throwable): CreateCredentialException {
        when (e) {
            is ApiException -> {
                when (e.statusCode) {
                    CommonStatusCodes.CANCELED -> {
                        return CreateCredentialCancellationException(e.message)
                    }
                    CommonStatusCodes.API_NOT_CONNECTED -> {
                        return CreateCredentialUnsupportedException(
                            "API is not supported: " + e.message
                        )
                    }
                    CommonStatusCodes.INTERNAL_ERROR -> {
                        return CreateCredentialNoCreateOptionException(e.message)
                    }
                    in retryables -> {
                        return CreateCredentialInterruptedException(e.message)
                    }
                    else -> {
                        return CreateCredentialUnknownException(
                            "Conditional create failed, failure: ${e.message}"
                        )
                    }
                }
            }
            is UnsupportedApiCallException -> {
                return CreateCredentialUnsupportedException("API is unsupported")
            }
        }
        return CreateCredentialUnknownException("Conditional create failed, failure: $e")
    }

    public override fun convertRequestToPlayServices(
        request: CreatePublicKeyCredentialRequest
    ): CreateCredentialRequest {
        return CreateCredentialRequest(
            type = request.type,
            credentialData = request.credentialData,
            candidateQueryData = request.candidateQueryData,
            origin = request.origin,
            requestJson = request.requestJson,
            resultReceiver = null,
        )
    }

    override fun convertResponseToCredentialManager(
        response: CreateCredentialResponse
    ): androidx.credentials.CreateCredentialResponse {
        return androidx.credentials.CreateCredentialResponse.createFrom(
            response.type,
            response.data,
        )
    }

    companion object {
        /**
         * Factory method for
         * [androidx.credentials.playservices.controllers.identityauth.createpublickeycredential.CredentialProviderCreatePublicKeyCredentialController].
         *
         * @param context the calling context for this controller
         * @return a credential provider controller for IdentityCredentialsCreatePublicKeyCredential
         */
        @JvmStatic
        fun getInstance(context: Context): CreatePublicKeyCredentialController {
            return CreatePublicKeyCredentialController(context)
        }

        private const val TAG = "CreatePublicKey"
    }
}
