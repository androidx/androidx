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

import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.people.peopleprovider.IsPeopleProviderSupportedRequest
import com.google.android.gms.people.peopleprovider.IsPeopleProviderSupportedResponse
import com.google.android.gms.people.peopleprovider.NotifyDirectoryChangeRequest
import com.google.android.gms.people.peopleprovider.NotifyDirectoryChangeResponse
import com.google.android.gms.people.peopleprovider.PeopleDirectoryCompatClient
import com.google.android.gms.people.peopleprovider.RequestDirectoryRefreshRequest
import com.google.android.gms.people.peopleprovider.RequestDirectoryRefreshResponse
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

/**
 * [PeopleDirectoryCompatClient] that records its calls and returns the results it was given,
 * standing in for Google Play services.
 */
internal class FakePeopleDirectoryCompatClient(
    private val notifyAccepted: Boolean = true,
    private val refreshAccepted: Boolean = true,
    /** When set, every call fails with it. */
    private val failure: Exception? = null,
) : PeopleDirectoryCompatClient {

    val notifyRequests = mutableListOf<NotifyDirectoryChangeRequest>()
    var refreshRequestCount: Int = 0
        private set

    override fun notifyDirectoryChange(
        request: NotifyDirectoryChangeRequest
    ): Task<NotifyDirectoryChangeResponse> = result {
        notifyRequests += request
        NotifyDirectoryChangeResponse(notifyAccepted)
    }

    override fun requestDirectoryRefresh(
        request: RequestDirectoryRefreshRequest
    ): Task<RequestDirectoryRefreshResponse> = result {
        refreshRequestCount++
        RequestDirectoryRefreshResponse(refreshAccepted)
    }

    override fun isPeopleProviderSupported(
        request: IsPeopleProviderSupportedRequest
    ): Task<IsPeopleProviderSupportedResponse> = result {
        IsPeopleProviderSupportedResponse(IsPeopleProviderSupportedResponse.BACKEND_GMS_CORE)
    }

    override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions> =
        throw UnsupportedOperationException()

    private inline fun <T> result(block: () -> T): Task<T> {
        failure?.let {
            return Tasks.forException(it)
        }
        return Tasks.forResult(block())
    }
}
