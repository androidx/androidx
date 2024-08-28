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
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CredentialProvider
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialProviderConfigurationException
import androidx.credentials.exceptions.ClearCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.playservices.controllers.BeginSignIn.CredentialProviderBeginSignInController
import androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.CredentialProviderCreatePublicKeyCredentialController
import androidx.credentials.playservices.controllers.CreateRestoreCredential.CredentialProviderCreateRestoreCredentialController
import androidx.credentials.playservices.controllers.GetRestoreCredential.CredentialProviderGetDigitalCredentialController
import androidx.credentials.playservices.controllers.GetRestoreCredential.CredentialProviderGetRestoreCredentialController
import androidx.credentials.playservices.controllers.GetSignInIntent.CredentialProviderGetSignInIntentController
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.blockstore.restorecredential.RestoreCredential
import com.google.android.gms.auth.blockstore.restorecredential.RestoreCredentialStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import java.util.concurrent.Executor

/** Entry point of all credential manager requests to the play-services-auth module. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("deprecation")
@OptIn(ExperimentalDigitalCredentialApi::class)
class CredentialProviderPlayServicesImpl(private val context: Context) : CredentialProvider {

    @VisibleForTesting var googleApiAvailability = GoogleApiAvailability.getInstance()

    override fun onGetCredential(
        context: Context,
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
    ) {
        if (cancellationReviewer(cancellationSignal)) {
            return
        }
        if (isDigitalCredentialRequest(request)) {
            if (!isAvailableOnDevice(MIN_GMS_APK_VERSION_DIGITAL_CRED)) {
                cancellationReviewerWithCallback(cancellationSignal) {
                    executor.execute {
                        callback.onError(
                            GetCredentialProviderConfigurationException(
                                "this device requires a Google Play Services update for the" +
                                    " given feature to be supported"
                            )
                        )
                    }
                }
                return
            }
            CredentialProviderGetDigitalCredentialController(context)
                .invokePlayServices(request, callback, executor, cancellationSignal)
        } else if (isGetRestoreCredentialRequest(request)) {
            if (!isAvailableOnDevice(MIN_GMS_APK_VERSION_RESTORE_CRED)) {
                cancellationReviewerWithCallback(cancellationSignal) {
                    executor.execute {
                        callback.onError(
                            GetCredentialProviderConfigurationException(
                                "getCredentialAsync no provider dependencies found - please ensure " +
                                    "the desired provider dependencies are added"
                            )
                        )
                    }
                }
                return
            }
            CredentialProviderGetRestoreCredentialController(context)
                .invokePlayServices(request, callback, executor, cancellationSignal)
        } else if (isGetSignInIntentRequest(request)) {
            CredentialProviderGetSignInIntentController(context)
                .invokePlayServices(request, callback, executor, cancellationSignal)
        } else {
            CredentialProviderBeginSignInController(context)
                .invokePlayServices(request, callback, executor, cancellationSignal)
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
        if (cancellationReviewer(cancellationSignal)) {
            return
        }
        when (request) {
            is CreatePasswordRequest -> {
                CredentialProviderCreatePasswordController.getInstance(context)
                    .invokePlayServices(request, callback, executor, cancellationSignal)
            }
            is CreatePublicKeyCredentialRequest -> {
                CredentialProviderCreatePublicKeyCredentialController.getInstance(context)
                    .invokePlayServices(request, callback, executor, cancellationSignal)
            }
            is CreateRestoreCredentialRequest -> {
                if (!isAvailableOnDevice(MIN_GMS_APK_VERSION_RESTORE_CRED)) {
                    cancellationReviewerWithCallback(cancellationSignal) {
                        executor.execute {
                            callback.onError(
                                CreateCredentialProviderConfigurationException(
                                    "createCredentialAsync no provider dependencies found - please ensure the " +
                                        "desired provider dependencies are added"
                                )
                            )
                        }
                    }
                    return
                }
                CredentialProviderCreateRestoreCredentialController(context)
                    .invokePlayServices(request, callback, executor, cancellationSignal)
            }
            else -> {
                throw UnsupportedOperationException(
                    "Create Credential request is unsupported, not password or " +
                        "publickeycredential"
                )
            }
        }
    }

    override fun isAvailableOnDevice(): Boolean {
        return isAvailableOnDevice(MIN_GMS_APK_VERSION)
    }

    fun isAvailableOnDevice(minApkVersion: Int): Boolean {
        val resultCode = isGooglePlayServicesAvailable(context, minApkVersion)
        val isSuccessful = resultCode == ConnectionResult.SUCCESS
        if (!isSuccessful) {
            val connectionResult = ConnectionResult(resultCode)
            Log.w(
                TAG,
                "Connection with Google Play Services was not " +
                    "successful. Connection result is: " +
                    connectionResult.toString()
            )
        }
        return isSuccessful
    }

    // https://developers.google.com/android/reference/com/google/android/gms/common/ConnectionResult
    // There is one error code that supports retry API_DISABLED_FOR_CONNECTION but it would not
    // be useful to retry that one because our connection to GMSCore is a static variable
    // (see GoogleApiAvailability.getInstance()) so we cannot recreate the connection to retry.
    private fun isGooglePlayServicesAvailable(context: Context, minApkVersion: Int): Int {
        return googleApiAvailability.isGooglePlayServicesAvailable(
            context,
            /*minApkVersion=*/ minApkVersion
        )
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
        if (request.requestType == TYPE_CLEAR_RESTORE_CREDENTIAL) {
            if (!isAvailableOnDevice(MIN_GMS_APK_VERSION_RESTORE_CRED)) {
                cancellationReviewerWithCallback(cancellationSignal) {
                    executor.execute {
                        callback.onError(
                            ClearCredentialProviderConfigurationException(
                                "clearCredentialStateAsync no provider dependencies found - please ensure the " +
                                    "desired provider dependencies are added"
                            )
                        )
                    }
                }
                return
            }
            RestoreCredential.getRestoreCredentialClient(context)
                .clearRestoreCredential(
                    com.google.android.gms.auth.blockstore.restorecredential
                        .ClearRestoreCredentialRequest(request.requestBundle)
                )
                .addOnSuccessListener {
                    cancellationReviewerWithCallback(cancellationSignal) {
                        Log.i(TAG, "Cleared restore credential successfully!")
                        executor.execute { callback.onResult(null) }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Clearing restore credential failed", e)
                    var clearException: ClearCredentialException =
                        ClearCredentialUnknownException(
                            "Clear restore credential failed for unknown reason."
                        )
                    if (e is ApiException) {
                        when (e.statusCode) {
                            RestoreCredentialStatusCodes.RESTORE_CREDENTIAL_INTERNAL_FAILURE -> {
                                clearException =
                                    ClearCredentialUnknownException(
                                        "The restore credential internal service had a failure."
                                    )
                            }
                        }
                    }
                    cancellationReviewerWithCallback(cancellationSignal) {
                        executor.execute { callback.onError(clearException) }
                    }
                }
        } else {
            Identity.getSignInClient(context)
                .signOut()
                .addOnSuccessListener {
                    cancellationReviewerWithCallback(
                        cancellationSignal,
                        {
                            Log.i(TAG, "During clear credential, signed out successfully!")
                            executor.execute { callback.onResult(null) }
                        }
                    )
                }
                .addOnFailureListener { e ->
                    run {
                        cancellationReviewerWithCallback(
                            cancellationSignal,
                            {
                                Log.w(TAG, "During clear credential sign out failed with $e")
                                executor.execute {
                                    callback.onError(ClearCredentialUnknownException(e.message))
                                }
                            }
                        )
                    }
                }
        }
    }

    companion object {
        private const val TAG = "PlayServicesImpl"

        // This points to the min APK version of GMS that contains required changes
        // to make passkeys work well
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) const val MIN_GMS_APK_VERSION = 230815045
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val MIN_GMS_APK_VERSION_RESTORE_CRED = 242200000
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val MIN_GMS_APK_VERSION_DIGITAL_CRED = 243100000

        internal fun cancellationReviewerWithCallback(
            cancellationSignal: CancellationSignal?,
            callback: () -> Unit,
        ) {
            if (!cancellationReviewer(cancellationSignal)) {
                callback()
            }
        }

        internal fun cancellationReviewer(cancellationSignal: CancellationSignal?): Boolean {
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

        internal fun isGetRestoreCredentialRequest(request: GetCredentialRequest): Boolean {
            for (option in request.credentialOptions) {
                if (option is GetRestoreCredentialOption) {
                    return true
                }
            }
            return false
        }

        internal fun isDigitalCredentialRequest(request: GetCredentialRequest): Boolean {
            for (option in request.credentialOptions) {
                if (option is GetDigitalCredentialOption) {
                    return true
                }
            }
            return false
        }
    }
}
