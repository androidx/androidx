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
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialRequest.Companion.BUNDLE_KEY_REQUEST_JSON
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Request to begin registering a public key credential, received by the provider with a
 *
 * This request will not contain all parameters needed to create the public key. Provider must
 * use the initial parameters to determine if the public key can be registered, and return
 * a list of [CreateEntry], denoting the accounts/groups where the public key can be registered.
 * When user selects one of the returned [CreateEntry], the corresponding [PendingIntent] set on
 * the [CreateEntry] will be fired. The [Intent] invoked through the [PendingIntent] will contain
 * the complete [CreatePublicKeyCredentialRequest]. This request will contain all required
 * parameters to actually register a public key.
 *
 * @property json the request json to be used for registering the public key credential
 * @property callingAppInfo the information of the calling app for which the passwords needs to
 * be stored
 *
 * @see BeginCreateCredentialProviderRequest
 *
 * @hide
 */
@RequiresApi(34)
class BeginCreatePublicKeyCredentialRequest internal constructor(
    val json: String,
    callingAppInfo: CallingAppInfo
) : BeginCreateCredentialProviderRequest(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    callingAppInfo) {

    init {
        require(json.isNotEmpty()) { "json must not be empty" }
    }

    /** @hide */
    companion object {
        @JvmStatic
        internal fun createFrom(data: Bundle, callingAppInfo: CallingAppInfo):
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