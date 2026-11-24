/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people.playservices

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.people.internal.PeopleBackend
import androidx.people.internal.PeopleBackendFactory
import androidx.people.internal.UnavailablePeopleBackend
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.UnsupportedApiCallException
import com.google.android.gms.people.peopleprovider.IsPeopleProviderSupportedRequest
import com.google.android.gms.people.peopleprovider.IsPeopleProviderSupportedResponse
import com.google.android.gms.people.peopleprovider.PeopleDirectoryCompatClient
import com.google.android.gms.people.peopleprovider.PeopleProviderCompatClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Entry point that `androidx.people:people` loads by name when this artifact is part of the
 * application, so that Google Play services can select the People Provider backend for client
 * applications and People Directory applications.
 *
 * Do not reference this class directly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PlayServicesPeopleBackendFactory
internal constructor(
    private val checkGooglePlayServicesAvailability: (Context) -> Int,
    private val createProviderClient: (Context) -> PeopleProviderCompatClient,
    private val createDirectoryClient: (Context) -> PeopleDirectoryCompatClient,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PeopleBackendFactory {

    /** Called reflectively by `androidx.people:people`, and kept by this artifact's keep rule. */
    public constructor() :
        this(
            checkGooglePlayServicesAvailability = { context ->
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            },
            createProviderClient = { context -> PeopleProviderCompatClient.getClient(context) },
            createDirectoryClient = { context -> PeopleDirectoryCompatClient.getClient(context) },
        )

    override suspend fun create(context: Context): PeopleBackend? {
        // A missing, disabled, or outdated Google Play services cannot select a backend, so fall
        // back to the platform People Provider for this process. An update in progress
        // (SERVICE_UPDATING) is transient, so throw to retry on the next call.
        when (withContext(coroutineDispatcher) { checkGooglePlayServicesAvailability(context) }) {
            ConnectionResult.SUCCESS -> Unit
            ConnectionResult.SERVICE_UPDATING ->
                throw IllegalStateException("Google Play services is currently updating")
            else -> return null
        }
        val providerClient = createProviderClient(context)
        // One call answers for client applications and People Directory applications alike:
        // Google Play services checks its rollout and the calling application.
        val backendType =
            try {
                providerClient
                    .isPeopleProviderSupported(IsPeopleProviderSupportedRequest.builder().build())
                    .await()
                    .supportedBackendType
            } catch (e: Exception) {
                // When Google Play services lacks the People Provider API, fall back to the
                // platform People Provider for this process. Any transient or unexpected failure,
                // including cancellation and transient connection errors wrapped in
                // API_NOT_CONNECTED, is rethrown so that androidx.people:people does not cache the
                // answer and asks again on the next call.
                val connectionErrorCode = (e as? ApiException)?.status?.connectionResult?.errorCode
                val gmsLacksPeopleProviderSupport =
                    e is UnsupportedApiCallException ||
                        (e is ApiException &&
                            e.statusCode == CommonStatusCodes.API_NOT_CONNECTED &&
                            connectionErrorCode != null &&
                            connectionErrorCode !in TRANSIENT_CONNECTION_ERRORS)
                if (!gmsLacksPeopleProviderSupport) throw e
                Log.i(TAG, "Google Play services version does not support the People Provider", e)
                return null
            }
        return when (backendType) {
            IsPeopleProviderSupportedResponse.BACKEND_GMS_CORE ->
                PlayServicesPeopleBackend(
                    providerClient,
                    createDirectoryClient(context),
                    context.packageName,
                )
            IsPeopleProviderSupportedResponse.BACKEND_AOSP_PLATFORM -> null
            // Google Play services is present and supported, and did not select itself or the
            // platform (BACKEND_UNAVAILABLE, BACKEND_UNKNOWN, or an unrecognized type): honor that
            // response without falling back to the platform (returning null would fall back).
            else -> UnavailablePeopleBackend
        }
    }

    private companion object {
        const val TAG = "PlayServicesPeople"

        val TRANSIENT_CONNECTION_ERRORS =
            setOf(
                ConnectionResult.INTERNAL_ERROR,
                ConnectionResult.NETWORK_ERROR,
                ConnectionResult.TIMEOUT,
                ConnectionResult.INTERRUPTED,
                ConnectionResult.SERVICE_UPDATING,
                ConnectionResult.CANCELED,
                ConnectionResult.API_DISABLED,
                ConnectionResult.API_DISABLED_FOR_CONNECTION,
            )
    }
}
