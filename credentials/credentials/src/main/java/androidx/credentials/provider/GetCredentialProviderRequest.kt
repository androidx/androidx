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
package androidx.credentials.provider

import android.app.PendingIntent
import android.util.ArraySet
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialOption
import androidx.credentials.GetCredentialRequest

/**
 * Request received by the provider after the query phase of the get flow is complete and the
 * user has made a selection from the list of [CredentialEntry] that was set on the
 * [BeginGetCredentialsProviderResponse].
 *
 * This request will be added to the intent that starts the activity invoked by the [PendingIntent]
 * set on the [CredentialEntry] that the user selected. The request can be extracted by using
 * the PendingIntentHandler.
 *
 * @property callingAppRequest an instance of [GetCredentialRequest] that contains the credential
 * type request parameters for the final credential request
 * @property callingAppInfo information pertaining to the calling application
 *
 * @hide
 */
@RequiresApi(34)
class GetCredentialProviderRequest internal constructor(
    val callingAppRequest: GetCredentialRequest,
    val callingAppInfo: CallingAppInfo
    ) {
    companion object {
        internal fun createFrom(request: android.service.credentials.GetCredentialRequest):
        GetCredentialProviderRequest {
            val options = ArrayList<GetCredentialOption>()
            request.getCredentialOptions.forEach {
                options.add(GetCredentialOption.createFrom(
                    it.type, it.candidateQueryData,
                    it.credentialRetrievalData, it.requireSystemProvider()))
            }
            return GetCredentialProviderRequest(
                GetCredentialRequest.Builder()
                    .setGetCredentialOptions(options)
                    .setAutoSelectAllowed(false)
                    .build(),
                CallingAppInfo(
                    request.callingPackage,
                    ArraySet()
                ))
        }
    }
}