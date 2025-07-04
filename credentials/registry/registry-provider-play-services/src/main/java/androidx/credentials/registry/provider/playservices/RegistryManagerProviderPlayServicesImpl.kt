/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.registry.provider.playservices

import android.content.Context
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.registry.provider.ClearCredentialRegistryException
import androidx.credentials.registry.provider.ClearCredentialRegistryRequest
import androidx.credentials.registry.provider.ClearCredentialRegistryResponse
import androidx.credentials.registry.provider.ClearCredentialRegistryUnknownException
import androidx.credentials.registry.provider.RegisterCredentialsException
import androidx.credentials.registry.provider.RegisterCredentialsRequest
import androidx.credentials.registry.provider.RegisterCredentialsResponse
import androidx.credentials.registry.provider.RegisterCredentialsUnknownException
import androidx.credentials.registry.provider.RegistryManagerProvider
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.identitycredentials.ClearRegistryRequest
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import com.google.android.gms.identitycredentials.RegistrationRequest
import java.util.concurrent.Executor

/** Entry point of all credential manager requests to the play-services-auth module. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RegistryManagerProviderPlayServicesImpl(private val context: Context) :
    RegistryManagerProvider {
    @VisibleForTesting internal var googleApiAvailability = GoogleApiAvailability.getInstance()

    private val client = IdentityCredentialManager.getClient(context)

    override fun onRegisterCredentials(
        request: RegisterCredentialsRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback:
            CredentialManagerCallback<RegisterCredentialsResponse, RegisterCredentialsException>,
    ) {
        val gmsRequest =
            RegistrationRequest(
                credentials = request.credentials,
                matcher = request.matcher,
                type = request.type,
                requestType = "",
                protocolTypes = emptyList(),
                id = request.id,
                fulfillmentActionName = request.intentAction,
            )
        client
            .registerCredentials(gmsRequest)
            .addOnSuccessListener {
                // TODO: b/355652174 - convert this more generically from the parent abstract class
                callback.onResult(object : RegisterCredentialsResponse(request.type) {})
            }
            .addOnFailureListener {
                callback.onError(RegisterCredentialsUnknownException(it.message))
            }
    }

    override fun onClearCredentialRegistry(
        request: ClearCredentialRegistryRequest,
        executor: Executor,
        callback:
            CredentialManagerCallback<
                ClearCredentialRegistryResponse,
                ClearCredentialRegistryException,
            >,
    ) {
        val gmsRequest =
            ClearRegistryRequest(
                deleteAll = request.isDeleteAll,
                clearTypedRegistryOption =
                    request.deletePerTypeConfig?.let {
                        ClearRegistryRequest.ClearTypedRegistryOption(
                            deleteAllForType = it.isDeleteAll,
                            type = it.type,
                            deleteIdlessRegistry = false,
                            registryIds = it.registryIds,
                        )
                    },
            )
        client
            .clearRegistry(gmsRequest)
            .addOnSuccessListener {
                callback.onResult(ClearCredentialRegistryResponse(it.isDeleted))
            }
            .addOnFailureListener {
                callback.onError(ClearCredentialRegistryUnknownException(it.message))
            }
    }

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

    private companion object {
        const val MIN_GMS_APK_VERSION = 243100000
    }
}
