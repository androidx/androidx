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
import android.content.Intent
import android.os.Bundle
import android.service.credentials.CallingAppInfo
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_REQUEST_JSON
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Request to begin registering a public key credential.
 *
 * This request will not contain all parameters needed to create the public key. Provider must
 * use the initial parameters to determine if the public key can be registered, and return
 * a list of [CreateEntry], denoting the accounts/groups where the public key can be registered.
 * When user selects one of the returned [CreateEntry], the corresponding [PendingIntent] set on
 * the [CreateEntry] will be fired. The [Intent] invoked through the [PendingIntent] will contain
 * the complete [CreatePublicKeyCredentialRequest]. This request will contain all required
 * parameters to actually register a public key.
 *
 * @property requestJson the request json to be used for registering the public key credential
 *
 * @see BeginCreateCredentialRequest
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class BeginCreatePublicKeyCredentialRequest constructor(
    val requestJson: String,
    callingAppInfo: CallingAppInfo?,
) : BeginCreateCredentialRequest(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    // TODO ("Use custom version of toCandidateData in this class")
    toCandidateDataBundle(
        requestJson,
        /*preferImmediatelyAvailableCredentials=*/false
    ),
    callingAppInfo
) {
    init {
        require(requestJson.isNotEmpty()) { "json must not be empty" }
    }

    /** @hide **/
    @Suppress("AcronymName")
    companion object {
        /** @hide */
        @JvmStatic
        internal fun toCandidateDataBundle(
            requestJson: String,
            preferImmediatelyAvailableCredentials: Boolean
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                CreatePublicKeyCredentialRequest
                    .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST
            )
            bundle.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
            bundle.putBoolean(
                CreatePublicKeyCredentialRequest
                    .BUNDLE_KEY_PREFER_IMMEDIATELY_AVAILABLE_CREDENTIALS,
                preferImmediatelyAvailableCredentials
            )
            return bundle
        }

        /** @hide */
        @JvmStatic
        internal fun createFrom(data: Bundle, callingAppInfo: CallingAppInfo?):
            BeginCreatePublicKeyCredentialRequest {
            try {
                val requestJson = data.getString(BUNDLE_KEY_REQUEST_JSON)
                return BeginCreatePublicKeyCredentialRequest(requestJson!!, callingAppInfo)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}