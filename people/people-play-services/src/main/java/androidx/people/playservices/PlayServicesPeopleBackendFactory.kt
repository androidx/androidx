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
 * application, so that client applications and People Directory applications can use the Google
 * Play services backport of the People Provider below Android 37.2.
 *
 * Do not reference this class directly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PlayServicesPeopleBackendFactory
internal constructor(
    private val isGooglePlayServicesAvailable: (Context) -> Boolean,
    private val createProviderClient: (Context) -> PeopleProviderCompatClient,
    private val createDirectoryClient: (Context) -> PeopleDirectoryCompatClient,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PeopleBackendFactory {

    /** Called reflectively by `androidx.people:people`, and kept by this artifact's keep rule. */
    public constructor() :
        this(
            isGooglePlayServicesAvailable = { context ->
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
                    ConnectionResult.SUCCESS
            },
            createProviderClient = { context -> PeopleProviderCompatClient.getClient(context) },
            createDirectoryClient = { context -> PeopleDirectoryCompatClient.getClient(context) },
        )

    override suspend fun create(context: Context): PeopleBackend? {
        // androidx.people:people asks only when the platform People Provider is absent. A missing,
        // disabled, or outdated Google Play services is definitive until it is updated, which
        // restarts the process, so it is not retried.
        if (!withContext(coroutineDispatcher) { isGooglePlayServicesAvailable(context) }) {
            return null
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
                // A Google Play services version without the People Provider API is definitive
                // until it is updated, as above. Any other failure, including cancellation, is
                // rethrown so that androidx.people:people does not cache the answer and asks again
                // on the next call.
                val gmsLacksPeopleProviderSupport =
                    e is UnsupportedApiCallException ||
                        (e is ApiException && e.statusCode == CommonStatusCodes.API_NOT_CONNECTED)
                if (!gmsLacksPeopleProviderSupport) throw e
                Log.i(TAG, "Google Play services version does not support the People Provider", e)
                return null
            }
        // Any other type (BACKEND_UNAVAILABLE, BACKEND_AOSP_PLATFORM, BACKEND_UNKNOWN, or one
        // added by a newer Google Play services) means that Google Play services does not serve
        // the application, and the platform was already found absent.
        // TODO(b/562550480): Honor the backend that Google Play services names, including on
        //  devices with the platform People Provider: query this factory before the platform, use
        //  the platform for BACKEND_AOSP_PLATFORM, and no People Provider for BACKEND_UNAVAILABLE.
        if (backendType != IsPeopleProviderSupportedResponse.BACKEND_GMS_CORE) return null
        return PlayServicesPeopleBackend(
            providerClient,
            createDirectoryClient(context),
            context.packageName,
        )
    }

    private companion object {
        const val TAG = "PlayServicesPeople"
    }
}
