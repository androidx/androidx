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

package androidx.credentials.playservices

import android.app.Activity
import android.os.CancellationSignal
import android.util.Log
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CredentialProvider
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController
import java.util.concurrent.Executor

/**
 * Entry point of all credential manager requests to the play-services-auth
 * module.
 *
 * @hide
 */
@Suppress("deprecation")
class CredentialProviderPlayServicesImpl : CredentialProvider {
    override fun onGetCredential(
        request: GetCredentialRequest,
        activity: Activity?,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse>
    ) {
        if (cancellationSignal != null) {
            Log.i(TAG, "onCreateCredential cancellationSignal not used")
            TODO("Use Cancel Operations Properly")
        }
        TODO("Not yet implemented")
    }

    override fun onCreateCredential(
        request: CreateCredentialRequest,
        activity: Activity?,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse>
    ) {
        if (cancellationSignal != null) {
            Log.i(TAG, "onCreateCredential cancellationSignal not used")
            TODO("Use Cancel Operations Properly")
        }
        val fragmentManager: android.app.FragmentManager = activity!!.fragmentManager
        // TODO("Manage Fragment Lifecycle and Fragment Manager Properly")
        if (request is CreatePasswordRequest) {
            CredentialProviderCreatePasswordController.getInstance(
                fragmentManager).invokePlayServices(
                request,
                callback,
                executor)
        } else if (request is CreatePublicKeyCredentialRequest) {
            TODO("Not yet implemented")
        } else {
            throw UnsupportedOperationException(
                "Unsupported request; not password or publickeycredential")
        }
    }

    override fun isAvailableOnDevice(): Boolean {
        TODO("Not yet implemented")
    }

    companion object {
        private val TAG = CredentialProviderPlayServicesImpl::class.java.name
    }
}