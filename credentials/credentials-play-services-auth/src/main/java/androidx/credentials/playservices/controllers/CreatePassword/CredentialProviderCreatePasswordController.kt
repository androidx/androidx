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

package androidx.credentials.playservices.controllers.CreatePassword

import android.content.Intent
import android.util.Log
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.auth.api.identity.SavePasswordRequest
import java.util.concurrent.Executor

/**
 * A controller to handle the CreatePassword flow with play services.
 *
 * @hide
 */
@Suppress("deprecation")
class CredentialProviderCreatePasswordController : CredentialProviderController<
        CreatePasswordRequest,
        SavePasswordRequest,
        Intent,
        CreateCredentialResponse>() {

    /**
     * The callback object state, used in the protected handleResponse method.
     */
    private lateinit var callback: CredentialManagerCallback<CreateCredentialResponse>

    /**
     * The callback requires an executor to invoke it.
     */
    private lateinit var executor: Executor

    override fun invokePlayServices(
        request: CreatePasswordRequest,
        callback: CredentialManagerCallback<CreateCredentialResponse>,
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

    override fun convertToPlayServices(request: CreatePasswordRequest): SavePasswordRequest {
        TODO("Not yet implemented")
    }

    override fun convertToCredentialProvider(response: Intent): CreateCredentialResponse {
        TODO("Not yet implemented")
    }

    companion object {
        private val TAG = CredentialProviderCreatePasswordController::class.java.name
        private const val REQUEST_CODE_GIS_SAVE_PASSWORD: Int = 1
        // TODO("Ensure this works with the lifecycle")

        /**
         * This finds a past version of the
         * [CredentialProviderCreatePasswordController] if it exists, otherwise
         * it generates a new instance.
         *
         * @param fragmentManager a fragment manager pulled from an android activity
         * @return a credential provider controller for CreatePublicKeyCredential
         */
        @JvmStatic
        fun getInstance(fragmentManager: android.app.FragmentManager):
            CredentialProviderCreatePasswordController {
            var controller = findPastController(REQUEST_CODE_GIS_SAVE_PASSWORD, fragmentManager)
            if (controller == null) {
                controller = CredentialProviderCreatePasswordController()
                fragmentManager.beginTransaction().add(controller,
                    REQUEST_CODE_GIS_SAVE_PASSWORD.toString())
                    .commitAllowingStateLoss()
                fragmentManager.executePendingTransactions()
            }
            return controller
        }

        internal fun findPastController(
            requestCode: Int,
            fragmentManager: android.app.FragmentManager
        ): CredentialProviderCreatePasswordController? {
            try {
                return fragmentManager.findFragmentByTag(requestCode.toString())
                    as CredentialProviderCreatePasswordController
            } catch (e: Exception) {
                Log.i(TAG, "Old fragment found of different type - replacement required")
                // TODO("Ensure this is well tested for fragment issues")
                return null
            }
        }
    }
}