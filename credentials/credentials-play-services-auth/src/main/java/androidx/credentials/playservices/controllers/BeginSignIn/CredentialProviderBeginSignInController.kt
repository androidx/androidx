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

package androidx.credentials.playservices.controllers.BeginSignIn

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.credentials.Credential
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.playservices.HiddenActivity
import androidx.credentials.playservices.controllers.BeginSignIn.BeginSignInControllerUtility.Companion.constructBeginSignInRequest
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.PublicKeyCredentialControllerUtility
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import java.util.concurrent.Executor

/**
 * A controller to handle the BeginSignIn flow with play services.
 *
 * @hide
 */
@Suppress("deprecation")
class CredentialProviderBeginSignInController(private val activity: Activity) :
    CredentialProviderController<
    GetCredentialRequest,
    BeginSignInRequest,
    SignInCredential,
    GetCredentialResponse,
    GetCredentialException>(activity) {

    /**
     * The callback object state, used in the protected handleResponse method.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var callback: CredentialManagerCallback<GetCredentialResponse,
        GetCredentialException>
    /**
     * The callback requires an executor to invoke it.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var executor: Executor

    private val resultReceiver = object : ResultReceiver(
        Handler(Looper.getMainLooper())
    ) {
        public override fun onReceiveResult(
            resultCode: Int,
            resultData: Bundle
        ) {
            Log.i(TAG, "onReceiveResult - CredentialProviderBeginSignInController")
            if (maybeReportErrorFromResultReceiver(resultData,
                    CredentialProviderBaseController
                        .Companion::getCredentialExceptionTypeToException,
                    executor = executor, callback = callback)) return
            handleResponse(resultData.getInt(ACTIVITY_REQUEST_CODE_TAG), resultCode,
                resultData.getParcelable(RESULT_DATA_TAG))
        }
    }

    override fun invokePlayServices(
        request: GetCredentialRequest,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
        executor: Executor
    ) {
        this.callback = callback
        this.executor = executor
        val convertedRequest: BeginSignInRequest = this.convertRequestToPlayServices(request)
        val hiddenIntent = Intent(activity, HiddenActivity::class.java)
        hiddenIntent.putExtra(REQUEST_TAG, convertedRequest)
        generateHiddenActivityIntent(resultReceiver, hiddenIntent, BEGIN_SIGN_IN_TAG)
        activity.startActivity(hiddenIntent)
    }

    internal fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        if (uniqueRequestCode != CONTROLLER_REQUEST_CODE) {
            Log.i(TAG, "returned request code does not match what was given")
            return
        }
        if (maybeReportErrorResultCodeGet(resultCode, TAG) { e -> this.executor.execute {
                    this.callback.onError(e) } }
        ) return
        try {
            val signInCredential = Identity.getSignInClient(activity)
                .getSignInCredentialFromIntent(data)
            Log.i(
                TAG, "Credential returned : " + signInCredential.googleIdToken + " , " +
                    signInCredential.id + " , " + signInCredential.password
            )
            val response = convertResponseToCredentialManager(signInCredential)
            Log.i(TAG, "Credential : " + response.credential.toString())
            this.executor.execute { this.callback.onResult(response) }
        } catch (e: ApiException) {
            var exception: GetCredentialException = GetCredentialUnknownException(e.message)
            if (e.statusCode == CommonStatusCodes.CANCELED) {
                Log.i(TAG, "User cancelled the prompt!")
                exception = GetCredentialCancellationException(e.message)
            } else if (e.statusCode in retryables) {
                exception = GetCredentialInterruptedException(e.message)
            }
            executor.execute { ->
                callback.onError(
                    exception
                )
            }
            return
        } catch (e: GetCredentialException) {
            executor.execute { ->
                callback.onError(
                    e
                )
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun convertRequestToPlayServices(request: GetCredentialRequest):
        BeginSignInRequest {
        return constructBeginSignInRequest(request)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun convertResponseToCredentialManager(response: SignInCredential):
        GetCredentialResponse {
        var cred: Credential? = null
        if (response.password != null) {
            cred = PasswordCredential(response.id, response.password!!)
        } else if (response.googleIdToken != null) {
            TODO(" Implement GoogleIdTokenVersion")
        } else if (response.publicKeyCredential != null) {
            cred = PublicKeyCredential(
                PublicKeyCredentialControllerUtility.toAssertPasskeyResponse(response))
        } else {
            Log.i(TAG, "Credential returned but no google Id or password or passkey found")
        }
        if (cred == null) {
            throw GetCredentialUnknownException("null credential found")
        }
        return GetCredentialResponse(cred)
    }

    companion object {
        private val TAG = CredentialProviderBeginSignInController::class.java.name
        private var controller: CredentialProviderBeginSignInController? = null
        // TODO("Ensure this is tested for multiple calls")

        /**
         * This finds a past version of the [CredentialProviderBeginSignInController] if it exists,
         * otherwise it generates a new instance.
         *
         * @param activity the calling activity for this controller
         * @return a credential provider controller for a specific begin sign in credential request
         */
        @JvmStatic
        fun getInstance(activity: Activity):
            CredentialProviderBeginSignInController {
            if (controller == null) {
                controller = CredentialProviderBeginSignInController(activity)
            }
            return controller!!
        }
    }
}