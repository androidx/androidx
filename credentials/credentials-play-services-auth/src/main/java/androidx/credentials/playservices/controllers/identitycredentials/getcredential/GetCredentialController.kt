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

package androidx.credentials.playservices.controllers.identitycredentials.getcredential

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
import androidx.credentials.Credential
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl.Companion.isGetSignInIntentRequest
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderController
import androidx.credentials.playservices.controllers.ResponseUtils
import androidx.credentials.playservices.controllers.identityauth.HiddenActivity
import androidx.credentials.playservices.controllers.identityauth.beginsignin.CredentialProviderBeginSignInController
import androidx.credentials.playservices.controllers.identityauth.getsigninintent.CredentialProviderGetSignInIntentController
import com.google.android.gms.identitycredentials.CredentialOption
import com.google.android.gms.identitycredentials.GetCredentialRequest
import com.google.android.gms.identitycredentials.GetCredentialResponse
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import java.util.concurrent.Executor

/** A controller to handle the get credential flow with identity credentials play services. */
@RequiresApi(Build.VERSION_CODES.M)
internal class GetCredentialController(val context: Context) :
    CredentialProviderController<
        androidx.credentials.GetCredentialRequest,
        GetCredentialRequest,
        GetCredentialResponse,
        androidx.credentials.GetCredentialResponse,
        GetCredentialException,
    >(context) {

    /** The callback object state, used in the protected handleResponse method. */
    @VisibleForTesting
    lateinit var callback:
        CredentialManagerCallback<
            androidx.credentials.GetCredentialResponse,
            GetCredentialException,
        >

    /** The callback requires an executor to invoke it. */
    @VisibleForTesting lateinit var executor: Executor

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
                            getCredentialExceptionTypeToException,
                        executor = executor,
                        callback = callback,
                        cancellationSignal,
                    )
                )
                    return
                ResponseUtils.handleGetCredentialResponse(
                    resultData.getInt(ACTIVITY_REQUEST_CODE_TAG),
                    resultCode,
                    getParcelable(resultData, RESULT_DATA_TAG, Intent::class.java),
                    executor,
                    callback,
                    cancellationSignal,
                )
            }
        }

    override fun invokePlayServices(
        request: androidx.credentials.GetCredentialRequest,
        callback:
            CredentialManagerCallback<
                androidx.credentials.GetCredentialResponse,
                GetCredentialException,
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
            .getCredential(convertedRequest)
            .addOnSuccessListener { result ->
                if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
                    return@addOnSuccessListener
                }
                val hiddenIntent = Intent(context, HiddenActivity::class.java)
                generateHiddenActivityIntent(resultReceiver, hiddenIntent, BEGIN_SIGN_IN_TAG)
                hiddenIntent.putExtra(EXTRA_FLOW_PENDING_INTENT, result.pendingIntent)
                try {
                    context.startActivity(hiddenIntent)
                } catch (_: Exception) {
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute {
                            callback.onError(
                                GetCredentialUnknownException(ERROR_MESSAGE_START_ACTIVITY_FAILED)
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isGetSignInIntentRequest(request)) {
                    Log.w(
                        TAG,
                        "Pre-u credman get flow failed for get sign in intent; retrying with gis flow",
                    )
                    CredentialProviderGetSignInIntentController(context)
                        .invokePlayServices(request, callback, executor, cancellationSignal)
                } else {
                    Log.w(TAG, "Pre-u credman get flow failed; retrying with gis flow")
                    CredentialProviderBeginSignInController(context)
                        .invokePlayServices(request, callback, executor, cancellationSignal)
                }
            }
    }

    public override fun convertRequestToPlayServices(
        request: androidx.credentials.GetCredentialRequest
    ): GetCredentialRequest {
        val bundle = androidx.credentials.GetCredentialRequest.getRequestMetadataBundle(request)
        return GetCredentialRequest(
            request.credentialOptions.map { it -> convertCredentialOptionToPlayServices(it) },
            bundle,
            request.origin,
            ResultReceiver(null),
        )
    }

    public override fun convertResponseToCredentialManager(
        response: GetCredentialResponse
    ): androidx.credentials.GetCredentialResponse {
        val credential = Credential.createFrom(response.credential.type, response.credential.data)
        return androidx.credentials.GetCredentialResponse(credential)
    }

    private fun convertCredentialOptionToPlayServices(
        option: androidx.credentials.CredentialOption
    ): CredentialOption {
        return CredentialOption(
            type = option.type,
            credentialRetrievalData = option.requestData,
            candidateQueryData = option.candidateQueryData,
            requestMatcher = "",
            requestType = "",
            protocolType = "",
        )
    }

    companion object {
        private const val TAG = "GetCredentialController"
    }
}
