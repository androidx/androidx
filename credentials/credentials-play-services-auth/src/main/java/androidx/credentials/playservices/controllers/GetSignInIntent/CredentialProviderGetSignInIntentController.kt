/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.playservices.controllers.GetSignInIntent

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
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.HiddenActivity
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.concurrent.Executor

/**
 * A controller to handle the GetSignInIntent flow with play services.
 */
@Suppress("deprecation")
internal class CredentialProviderGetSignInIntentController(private val context: Context) :
    CredentialProviderController<GetCredentialRequest, GetSignInIntentRequest,
        SignInCredential, GetCredentialResponse, GetCredentialException>(context) {

    /**
     * The callback object state, used in the protected handleResponse method.
     */
    @VisibleForTesting
    lateinit var callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>

    /**
     * The callback requires an executor to invoke it.
     */
    @VisibleForTesting
    lateinit var executor: Executor

    /**
     * The cancellation signal, which is shuttled around to stop the flow at any moment prior to
     * returning data.
     */
    @VisibleForTesting
    private var cancellationSignal: CancellationSignal? = null

    private val resultReceiver = object : ResultReceiver(
        Handler(Looper.getMainLooper())
    ) {
        public override fun onReceiveResult(
            resultCode: Int,
            resultData: Bundle
        ) {
            if (maybeReportErrorFromResultReceiver(
                    resultData,
                 CredentialProviderBaseController.Companion::getCredentialExceptionTypeToException,
                    executor = executor,
                    callback = callback,
                    cancellationSignal
                )
            ) return
            handleResponse(
                resultData.getInt(ACTIVITY_REQUEST_CODE_TAG),
                resultCode,
                resultData.getParcelable(RESULT_DATA_TAG)
            )
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

        try {
            val convertedRequest: GetSignInIntentRequest =
                this.convertRequestToPlayServices(request)

            val hiddenIntent = Intent(context, HiddenActivity::class.java)
            hiddenIntent.putExtra(REQUEST_TAG, convertedRequest)
            generateHiddenActivityIntent(resultReceiver, hiddenIntent, SIGN_IN_INTENT_TAG)
            context.startActivity(hiddenIntent)
        } catch (e: Exception) {
            when (e) {
                is GetCredentialUnsupportedException ->
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        this.executor.execute {
                            this.callback.onError(e)
                        }
                    }
                else ->
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        this.executor.execute {
                            this.callback.onError(
                                GetCredentialUnknownException(ERROR_MESSAGE_START_ACTIVITY_FAILED)
                            )
                        }
                    }
            }
        }
    }

    @VisibleForTesting
    public override fun convertRequestToPlayServices(request: GetCredentialRequest):
        GetSignInIntentRequest {
        if (request.credentialOptions.count() != 1) {
            throw GetCredentialUnsupportedException(
                "GetSignInWithGoogleOption cannot be combined with other options."
            )
        }
        val option = request.credentialOptions[0] as GetSignInWithGoogleOption
        return GetSignInIntentRequest.builder()
            .setServerClientId(option.serverClientId)
            .filterByHostedDomain(option.hostedDomainFilter)
            .setNonce(option.nonce)
            .build()
    }

    override fun convertResponseToCredentialManager(response: SignInCredential):
        GetCredentialResponse {
        var cred: Credential? = null
        if (response.googleIdToken != null) {
            cred = createGoogleIdCredential(response)
        } else {
            Log.w(TAG, "Credential returned but no google Id found")
        }
        if (cred == null) {
            throw GetCredentialUnknownException(
                "When attempting to convert get response, " + "null credential found"
            )
        }
        return GetCredentialResponse(cred)
    }

    @VisibleForTesting
    fun createGoogleIdCredential(response: SignInCredential): GoogleIdTokenCredential {
        var cred = GoogleIdTokenCredential.Builder().setId(response.id)
        try {
            cred.setIdToken(response.googleIdToken!!)
        } catch (e: Exception) {
            throw GetCredentialUnknownException(
                "When attempting to convert get response, " + "null Google ID Token found"
            )
        }

        if (response.displayName != null) {
            cred.setDisplayName(response.displayName)
        }

        if (response.givenName != null) {
            cred.setGivenName(response.givenName)
        }

        if (response.familyName != null) {
            cred.setFamilyName(response.familyName)
        }

        if (response.phoneNumber != null) {
            cred.setPhoneNumber(response.phoneNumber)
        }

        if (response.profilePictureUri != null) {
            cred.setProfilePictureUri(response.profilePictureUri)
        }

        return cred.build()
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
        if (maybeReportErrorResultCodeGet(
                resultCode,
                { s, f -> cancelOrCallbackExceptionOrResult(s, f) },
                { e ->
                    this.executor.execute {
                        this.callback.onError(e)
                    }
                },
                cancellationSignal
            )
        ) return
        try {
            val signInCredential =
                Identity.getSignInClient(context).getSignInCredentialFromIntent(data)
            val response = convertResponseToCredentialManager(signInCredential)
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                this.executor.execute {
                    this.callback.onResult(response)
                }
            }
        } catch (e: ApiException) {
            var exception: GetCredentialException = GetCredentialUnknownException(e.message)
            if (e.statusCode == CommonStatusCodes.CANCELED) {
                exception = GetCredentialCancellationException(e.message)
            } else if (e.statusCode in retryables) {
                exception = GetCredentialInterruptedException(e.message)
            }
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                executor.execute {
                    callback.onError(exception)
                }
            }
            return
        } catch (e: GetCredentialException) {
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                executor.execute {
                    callback.onError(e)
                }
            }
        } catch (t: Throwable) {
            val e = GetCredentialUnknownException(t.message)
            cancelOrCallbackExceptionOrResult(cancellationSignal) {
                executor.execute {
                    callback.onError(e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "GetSignInIntent"

        /**
         * Factory method for [CredentialProviderGetSignInIntentController].
         *
         * @param context the calling context for this controller
         * @return a credential provider controller for a specific begin sign in credential request
         */
        @JvmStatic
        fun getInstance(context: Context): CredentialProviderGetSignInIntentController {
                return CredentialProviderGetSignInIntentController(context)
            }
    }
}
