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
import android.service.credentials.CallingAppInfo
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialOption

/**
 * Request received by the provider after the query phase of the get flow is complete i.e. the user
 * was presented with a list of credentials, and the user has now made a selection from the list of
 * [android.service.credentials.CredentialEntry] presented on the selector UI.
 *
 * This request will be added to the intent extras of the activity invoked by the [PendingIntent]
 * set on the [android.service.credentials.CredentialEntry] that the user selected. The request
 * must be extracted using the [PendingIntentHandler.retrieveProviderGetCredentialRequest] helper
 * API.
 *
 * @property credentialOption the credential retrieval parameters
 * @property callingAppInfo information pertaining to the calling application
 */
@RequiresApi(34)
class ProviderGetCredentialRequest internal constructor(
    val credentialOption: CredentialOption,
    val callingAppInfo: CallingAppInfo
    ) {

    /** @hide */
    companion object {
        internal fun createFrom(request: android.service.credentials.GetCredentialRequest):
        ProviderGetCredentialRequest {
            val option = CredentialOption.createFrom(
                request.getCredentialOption.type,
                request.getCredentialOption.candidateQueryData,
                request.getCredentialOption.credentialRetrievalData,
                request.getCredentialOption.isSystemProviderRequired
            )
            return ProviderGetCredentialRequest(
                option,
                request.callingAppInfo)
        }
    }
}