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
import androidx.annotation.VisibleForTesting
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_CLIENT_DATA_HASH
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_REQUEST_JSON
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.credentials.provider.utils.RequestValidationUtil

/**
 * Request to begin registering a public key credential.
 *
 * This request will not contain all parameters needed to create the public key. Provider must
 * use the initial parameters to determine if the public key can be created, and return a
 * [BeginCreateCredentialResponse] containing a list of [CreateEntry], denoting the
 * accounts/groups where the public key can be registered.
 * When user selects one of the returned [CreateEntry], the corresponding [PendingIntent] set on
 * the [CreateEntry] will be fired. The [Intent] invoked through the [PendingIntent] will contain
 * the complete [CreatePublicKeyCredentialRequest]. This request will contain all required
 * parameters to actually register a public key.
 *
 * @see BeginCreateCredentialRequest
 *
 * Note: Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 *
 * @constructor constructs an instance of [BeginCreatePublicKeyCredentialRequest]
 * @param requestJson the request json to be used for registering the public key credential
 * @param callingAppInfo the information pertaining to the app requesting for credentials
 * @param candidateQueryData the raw bundle containing the request parameters, not expected to
 * be used directly as all basic parameters are parsed into structured properties in this class
 * like [requestJson] and [clientDataHash]
 * @param clientDataHash a hash that is used to verify the relying party identity, set only if
 * [android.service.credentials.CallingAppInfo.getOrigin] is set
 *
 * @throws NullPointerException If [requestJson] or [candidateQueryData] is null
 * @throws IllegalArgumentException If [requestJson] is empty, or is not a valid JSON
 */
class BeginCreatePublicKeyCredentialRequest @JvmOverloads constructor(
    val requestJson: String,
    callingAppInfo: CallingAppInfo?,
    candidateQueryData: Bundle,
    val clientDataHash: ByteArray? = null,
) : BeginCreateCredentialRequest(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    candidateQueryData,
    callingAppInfo
) {
    init {
        require(RequestValidationUtil.isValidJSON(requestJson)) { "requestJson must not " +
            "be empty, and must be a valid JSON" }
        initiateBundle(candidateQueryData, requestJson)
    }

    private fun initiateBundle(candidateQueryData: Bundle, requestJson: String) {
        candidateQueryData.putString(BUNDLE_KEY_REQUEST_JSON, requestJson)
    }

    internal companion object {

        @JvmStatic
        @VisibleForTesting
        fun createForTest(data: Bundle, callingAppInfo: CallingAppInfo?):
            BeginCreatePublicKeyCredentialRequest {
            return createFrom(data, callingAppInfo)
        }

        internal fun createFrom(data: Bundle, callingAppInfo: CallingAppInfo?):
            BeginCreatePublicKeyCredentialRequest {
            try {
                val requestJson = data.getString(BUNDLE_KEY_REQUEST_JSON)
                val clientDataHash = data.getByteArray(BUNDLE_KEY_CLIENT_DATA_HASH)
                return BeginCreatePublicKeyCredentialRequest(requestJson!!,
                    callingAppInfo, data, clientDataHash)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}
