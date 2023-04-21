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
import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.annotation.VisibleForTesting
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CredentialProvider
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.playservices.controllers.BeginSignIn.CredentialProviderBeginSignInController
import androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.CredentialProviderCreatePublicKeyCredentialController
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.util.concurrent.Executor

/**
 * Entry point of all credential manager requests to the play-services-auth
 * module.
 *
 * @hide
 */
@Suppress("deprecation")
class CredentialProviderPlayServicesImpl(private val context: Context) : CredentialProvider {

    @VisibleForTesting
    var googleApiAvailability = GoogleApiAvailability.getInstance()
    override fun onGetCredential(
        request: GetCredentialRequest,
        activity: Activity,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
    ) {
        if (cancellationReviewer(cancellationSignal)) { return }
        CredentialProviderBeginSignInController(activity).invokePlayServices(
            request, callback, executor, cancellationSignal)
    }

    @SuppressWarnings("deprecated")
    override fun onCreateCredential(
        request: CreateCredentialRequest,
        activity: Activity,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>
    ) {
        if (cancellationReviewer(cancellationSignal)) { return }
        when (request) {
            is CreatePasswordRequest -> {
                CredentialProviderCreatePasswordController.getInstance(
                    activity).invokePlayServices(
                    request,
                    callback,
                    executor,
                    cancellationSignal)
            }
            is CreatePublicKeyCredentialRequest -> {
                CredentialProviderCreatePublicKeyCredentialController.getInstance(
                    activity).invokePlayServices(
                    request,
                    callback,
                    executor,
                    cancellationSignal)
            }
            else -> {
                throw UnsupportedOperationException(
                    "Create Credential request is unsupported, not password or " +
                        "publickeycredential")
            }
        }
    }
    override fun isAvailableOnDevice(): Boolean {
        val resultCode = isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    // https://developers.google.com/android/reference/com/google/android/gms/common/ConnectionResult
    // TODO(b/262924507): Most codes indicate failure, but two indicate retry-ability -
    //  look into handling.
    private fun isGooglePlayServicesAvailable(context: Context): Int {
        return googleApiAvailability.isGooglePlayServicesAvailable(context)
    }

    override fun onClearCredential(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<Void?, ClearCredentialException>
    ) {
        if (cancellationReviewer(cancellationSignal)) {
            return
        }
        Identity.getSignInClient(context)
            .signOut()
            .addOnSuccessListener {
                var isCanceled = false
                cancellationSignal?.let {
                    isCanceled = cancellationSignal.isCanceled
                }
                if (!isCanceled) {
                    Log.i(TAG, "During clear credential, signed out successfully!")
                    executor.execute { callback.onResult(null) }
                }
            }
            .addOnFailureListener { e ->
                run {
                    var isCanceled = false
                    cancellationSignal?.let {
                        isCanceled = cancellationSignal.isCanceled
                    }
                    if (!isCanceled) {
                        Log.w(TAG, "During clear credential sign out failed with $e")
                        executor.execute {
                            callback.onError(ClearCredentialUnknownException(e.message))
                        }
                    }
                }
            }
    }

    companion object {
        private val TAG = CredentialProviderPlayServicesImpl::class.java.name

        internal fun cancellationReviewer(
            cancellationSignal: CancellationSignal?
        ): Boolean {
            if (cancellationSignal != null) {
                if (cancellationSignal.isCanceled) {
                    Log.i(TAG, "the flow has been canceled")
                    // TODO(b/262924507): See if there's a better way to message pass to avoid
                    //  if statements and to use a single listener instead
                    return true
                }
            } else {
                Log.i(TAG, "No cancellationSignal found")
            }
            return false
        }
    }
}