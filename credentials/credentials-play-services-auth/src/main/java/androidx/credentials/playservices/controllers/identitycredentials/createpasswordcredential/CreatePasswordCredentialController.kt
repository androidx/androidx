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

package androidx.credentials.playservices.controllers.identitycredentials.createpasswordcredential

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
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderController
import androidx.credentials.playservices.controllers.identityauth.HiddenActivity
import androidx.credentials.playservices.controllers.identityauth.createpassword.CredentialProviderCreatePasswordController
import androidx.credentials.provider.PendingIntentHandler
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import java.util.concurrent.Executor

/**
 * A controller to handle the Create password credential flow with identity credentials play
 * services.
 */
@RequiresApi(Build.VERSION_CODES.M)
internal class CreatePasswordCredentialController(val context: Context) :
    CredentialProviderController<
        androidx.credentials.CreatePasswordRequest,
        CreateCredentialRequest,
        Unit,
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
    private lateinit var executor: Executor

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
        request: CreatePasswordRequest,
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
        val convertedRequest = convertRequestToPlayServices(request)
        IdentityCredentialManager.getClient(context)
            .createCredential(convertedRequest)
            .addOnSuccessListener { result ->
                if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
                    return@addOnSuccessListener
                }

                val hiddenIntent = Intent(context, HiddenActivity::class.java)
                generateHiddenActivityIntent(resultReceiver, hiddenIntent, CREATE_PASSWORD_TAG)
                hiddenIntent.putExtra(EXTRA_FLOW_PENDING_INTENT, result.pendingIntent)
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
            .addOnFailureListener { e ->
                Log.w(TAG, "Pre-u credman create flow failed $e; retrying with gis flow")
                CredentialProviderCreatePasswordController(context)
                    .invokePlayServices(request, callback, executor, cancellationSignal)
            }
    }

    public override fun convertRequestToPlayServices(
        request: androidx.credentials.CreatePasswordRequest
    ): CreateCredentialRequest {
        return CreateCredentialRequest(
            request.type,
            request.credentialData,
            request.candidateQueryData,
            request.origin,
            null,
            null,
        )
    }

    override fun convertResponseToCredentialManager(
        response: Unit
    ): androidx.credentials.CreateCredentialResponse {
        return CreatePasswordResponse()
    }

    internal fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        if (uniqueRequestCode != CONTROLLER_REQUEST_CODE) {
            Log.w(
                TAG,
                "Returned request code " +
                    "${CONTROLLER_REQUEST_CODE} which does not match what was given $uniqueRequestCode",
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
                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
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

    companion object {
        /**
         * Factory method for
         * [androidx.credentials.playservices.controllers.identitycredentials.createpasswordcredential.CreatePasswordCredentialController].
         *
         * @param context the calling context for this controller
         * @return a credential provider controller for CreatePasswordCredentialController
         */
        @JvmStatic
        fun getInstance(context: Context): CreatePasswordCredentialController {
            return CreatePasswordCredentialController(context)
        }

        private const val TAG = "CreatePassword"
    }
}
