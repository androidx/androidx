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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Build
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
import androidx.credentials.playservices.controllers.BeginSignIn.BeginSignInControllerUtility.Companion.constructBeginSignInRequest
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.PublicKeyCredentialControllerUtility
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInResult
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
class CredentialProviderBeginSignInController : CredentialProviderController<
    GetCredentialRequest,
    BeginSignInRequest,
    SignInCredential,
    GetCredentialResponse,
    GetCredentialException>() {

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

    @SuppressLint("ClassVerificationFailure")
    override fun invokePlayServices(
        request: GetCredentialRequest,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
        executor: Executor
    ) {
        this.callback = callback
        this.executor = executor
        val convertedRequest: BeginSignInRequest = this.convertRequestToPlayServices(request)
        Identity.getSignInClient(activity)
            .beginSignIn(convertedRequest)
            .addOnSuccessListener { result: BeginSignInResult ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender,
                            REQUEST_CODE_BEGIN_SIGN_IN,
                            null, /* fillInIntent= */
                            0, /* flagsMask= */
                            0, /* flagsValue= */
                            0, /* extraFlags= */
                            null /* options= */
                        )
                    }
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI in beginSignIn: " +
                        e.localizedMessage
                    )
                    val exception: GetCredentialException = GetCredentialUnknownException(
                        e.localizedMessage)
                    executor.execute { ->
                        callback.onError(exception)
                    }
                }
            }
            .addOnFailureListener { e: Exception ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.i(TAG, "Failure in begin sign in call")
                if (e.localizedMessage != null) { Log.i(TAG, e.localizedMessage!!) }
                var exception: GetCredentialException = GetCredentialUnknownException()
                if (e is ApiException && e.statusCode in this.retryables) {
                    exception = GetCredentialInterruptedException(e.localizedMessage)
                }
                executor.execute { ->
                    callback.onError(
                        exception
                    )
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleResponse(requestCode, resultCode, data)
    }

    private fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        if (uniqueRequestCode != REQUEST_CODE_BEGIN_SIGN_IN) {
            Log.i(TAG, "returned request code does not match what was given")
            return
        }
        if (resultCode != Activity.RESULT_OK) {
            var exception: GetCredentialException = GetCredentialUnknownException()
            if (resultCode == Activity.RESULT_CANCELED) {
                exception = GetCredentialCancellationException()
            }
            this.executor.execute { -> this.callback.onError(exception) }
            return
        }
        try {
            val signInCredential = Identity.getSignInClient(activity as Activity)
                .getSignInCredentialFromIntent(data)
            Log.i(TAG, "Credential returned : " + signInCredential.googleIdToken + " , " +
                signInCredential.id + ", " + signInCredential.password)
            val response = convertResponseToCredentialManager(signInCredential)
            Log.i(TAG, "Credential : " + response.credential.toString())
            this.executor.execute { this.callback.onResult(response) }
        } catch (e: ApiException) {
            var exception: GetCredentialException = GetCredentialUnknownException()
            if (e.statusCode == CommonStatusCodes.CANCELED) {
                Log.i(TAG, "User cancelled the prompt!")
                exception = GetCredentialCancellationException()
            } else if (e.statusCode in this.retryables) {
                exception = GetCredentialInterruptedException()
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
        private const val REQUEST_CODE_BEGIN_SIGN_IN: Int = 1
        // TODO("Ensure this works with the lifecycle")

        /**
         * This finds a past version of the [CredentialProviderBeginSignInController] if it exists,
         * otherwise it generates a new instance.
         *
         * @param fragmentManager a fragment manager pulled from an android activity
         * @return a credential provider controller for a specific credential request
         */
        @JvmStatic
        fun getInstance(fragmentManager: android.app.FragmentManager):
            CredentialProviderBeginSignInController {
            var controller = findPastController(REQUEST_CODE_BEGIN_SIGN_IN, fragmentManager)
            if (controller == null) {
                controller = CredentialProviderBeginSignInController()
                fragmentManager.beginTransaction().add(controller,
                    REQUEST_CODE_BEGIN_SIGN_IN.toString())
                    .commitAllowingStateLoss()
                fragmentManager.executePendingTransactions()
            }
            return controller
        }

        internal fun findPastController(
            requestCode: Int,
            fragmentManager: android.app.FragmentManager
        ): CredentialProviderBeginSignInController? {
            val oldFragment = fragmentManager.findFragmentByTag(requestCode.toString())
            try {
                return oldFragment as CredentialProviderBeginSignInController
            } catch (e: Exception) {
                Log.i(TAG,
                    "Error with old fragment or null - replacement required")
                if (oldFragment != null) {
                    fragmentManager.beginTransaction().remove(oldFragment).commitAllowingStateLoss()
                }
                // TODO("Ensure this is well tested for fragment issues")
                return null
            }
        }
    }
}