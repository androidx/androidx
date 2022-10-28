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

import android.content.Intent
import android.util.Log
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInCredential
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
    GetCredentialResponse>() {

    /**
     * The callback object state, used in the protected handleResponse method.
     */
    private lateinit var callback: CredentialManagerCallback<GetCredentialResponse>
    /**
     * The callback requires an executor to invoke it.
     */
    private lateinit var executor: Executor

    override fun invokePlayServices(
        request: GetCredentialRequest,
        callback: CredentialManagerCallback<GetCredentialResponse>,
        executor: Executor
    ) {
        TODO("Not yet implemented")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleResponse(requestCode, resultCode, data)
    }

    private fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(TAG, "$uniqueRequestCode $resultCode $data")
        TODO("Not yet implemented")
    }

    override fun convertToPlayServices(request: GetCredentialRequest): BeginSignInRequest {
        TODO("Not yet implemented")
    }

    override fun convertToCredentialProvider(response: SignInCredential): GetCredentialResponse {
        TODO("Not yet implemented")
    }

    companion object {
        private val TAG = CredentialProviderBeginSignInController::class.java.name
        private const val REQUEST_CODE_BEGIN_SIGN_IN: Int = 1
        // TODO("Ensure this works with the lifecycle")

        /**
         * This finds a past version of the BeginSignInController if it exists, otherwise
         * it generates a new instance.
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
            try {
                return fragmentManager.findFragmentByTag(requestCode.toString())
                    as CredentialProviderBeginSignInController?
            } catch (e: Exception) {
                Log.i(TAG, "Old fragment found of different type - replacement required")
                // TODO("Ensure this is well tested for fragment issues")
                return null
            }
        }
    }
}