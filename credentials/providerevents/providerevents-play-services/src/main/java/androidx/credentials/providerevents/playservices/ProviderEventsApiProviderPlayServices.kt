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

package androidx.credentials.providerevents.playservices

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.providerevents.ProviderEventsApiProvider
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.exception.RegisterExportException
import androidx.credentials.providerevents.exception.RegisterExportUnknownErrorException
import androidx.credentials.providerevents.playservices.controller.ImportCredentialsController
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import androidx.credentials.providerevents.transfer.RegisterExportResponse
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import java.util.concurrent.Executor

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ProviderEventsApiProviderPlayServices(private val context: Context) :
    ProviderEventsApiProvider {
    private val googleApiAvailability = GoogleApiAvailability.getInstance()

    override fun isAvailable(): Boolean {
        return isAvailableOnDevice(MIN_GMS_APK_VERSION)
    }

    // https://developers.google.com/android/reference/com/google/android/gms/common/ConnectionResult
    // There is one error code that supports retry API_DISABLED_FOR_CONNECTION but it would not
    // be useful to retry that one because our connection to GMSCore is a static variable
    // (see GoogleApiAvailability.getInstance()) so we cannot recreate the connection to retry.
    private fun isGooglePlayServicesAvailable(context: Context, minApkVersion: Int): Int {
        return googleApiAvailability.isGooglePlayServicesAvailable(
            context,
            /*minApkVersion=*/ minApkVersion,
        )
    }

    private fun isAvailableOnDevice(minApkVersion: Int): Boolean {
        val resultCode = isGooglePlayServicesAvailable(context, minApkVersion)
        return resultCode == ConnectionResult.SUCCESS
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onImportCredentials(
        context: Context,
        request: ImportCredentialsRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback:
            CredentialManagerCallback<ProviderImportCredentialsResponse, ImportCredentialsException>,
    ) {
        val controller =
            ImportCredentialsController(context, request, cancellationSignal, executor, callback)
        controller.invokePlayServices()
    }

    override fun onRegisterExport(
        request: RegisterExportRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<RegisterExportResponse, RegisterExportException>,
    ) {
        val gmsRequest =
            com.google.android.gms.identitycredentials.RegisterExportRequest(
                matcher = request.exportMatcher,
                data = request.credentialBytes,
                id = REGISTRY_ID,
            )
        val client = IdentityCredentialManager.getClient(context)
        client
            .registerExport(gmsRequest)
            .addOnSuccessListener { callback.onResult(RegisterExportResponse()) }
            .addOnFailureListener {
                callback.onError(RegisterExportUnknownErrorException(it.message))
            }
    }

    private companion object {
        const val TAG = "ProviderEventsApi"
        // TODO(b/436712597): Bump this version when the UX is ready
        const val MIN_GMS_APK_VERSION = 253800000
        const val REGISTRY_ID = "credential_transfer"
    }
}
