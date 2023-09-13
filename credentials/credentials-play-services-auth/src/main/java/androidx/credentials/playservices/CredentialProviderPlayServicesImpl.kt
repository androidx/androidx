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

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.ClearCredentialStateRequest
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
import androidx.credentials.playservices.controllers.GetSignInIntent.CredentialProviderGetSignInIntentController
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import java.util.concurrent.Executor

/**
 * Entry point of all credential manager requests to the play-services-auth
 * module.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("deprecation")
class CredentialProviderPlayServicesImpl(private val context: Context) : CredentialProvider {

    @VisibleForTesting
    var googleApiAvailability = GoogleApiAvailability.getInstance()
    override fun onGetCredential(
        context: Context,
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
    ) {
        if (cancellationReviewer(cancellationSignal)) { return }
        if (isGetSignInIntentRequest(request)) {
            CredentialProviderGetSignInIntentController(context).invokePlayServices(
                request, callback, executor, cancellationSignal
            )
        } else {
            CredentialProviderBeginSignInController(context).invokePlayServices(
                request, callback, executor, cancellationSignal
            )
        }
    }

    @SuppressWarnings("deprecated")
    override fun onCreateCredential(
        context: Context,
        request: CreateCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>
    ) {
        if (cancellationReviewer(cancellationSignal)) { return }
        when (request) {
            is CreatePasswordRequest -> {
                CredentialProviderCreatePasswordController.getInstance(
                    context).invokePlayServices(
                    request,
                    callback,
                    executor,
                    cancellationSignal)
            }
            is CreatePublicKeyCredentialRequest -> {
                CredentialProviderCreatePublicKeyCredentialController.getInstance(
                    context).invokePlayServices(
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
    // There is one error code that supports retry API_DISABLED_FOR_CONNECTION but it would not
    // be useful to retry that one because our connection to GMSCore is a static variable
    // (see GoogleApiAvailability.getInstance()) so we cannot recreate the connection to retry.
    private fun isGooglePlayServicesAvailable(context: Context): Int {
        return googleApiAvailability.isGooglePlayServicesAvailable(context)
    }

    override fun onClearCredential(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<Void?, ClearCredentialException>
    ) {
        if (cancellationReviewer(cancellationSignal)) { return }
        Identity.getSignInClient(context)
            .signOut()
            .addOnSuccessListener {
                cancellationReviewerWithCallback(cancellationSignal, {
                    Log.i(TAG, "During clear credential, signed out successfully!")
                    executor.execute { callback.onResult(null) }
                })
            }
            .addOnFailureListener { e ->
                run {
                    cancellationReviewerWithCallback(cancellationSignal, {
                        Log.w(TAG, "During clear credential sign out failed with $e")
                        executor.execute {
                            callback.onError(ClearCredentialUnknownException(e.message))
                        }
                    })
                }
            }
    }

    companion object {
        private const val TAG = "PlayServicesImpl"

        internal fun cancellationReviewerWithCallback(
            cancellationSignal: CancellationSignal?,
            callback: () -> Unit,
        ) {
            if (!cancellationReviewer(cancellationSignal)) {
                callback()
            }
        }

        internal fun cancellationReviewer(
            cancellationSignal: CancellationSignal?
        ): Boolean {
            if (cancellationSignal != null) {
                if (cancellationSignal.isCanceled) {
                    Log.i(TAG, "the flow has been canceled")
                    return true
                }
            } else {
                Log.i(TAG, "No cancellationSignal found")
            }
            return false
        }

        internal fun isGetSignInIntentRequest(request: GetCredentialRequest): Boolean {
            for (option in request.credentialOptions) {
                if (option is GetSignInWithGoogleOption) {
                    return true
                }
            }
            return false
        }
    }
}
