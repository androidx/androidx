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
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.PasswordCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Request to begin saving a password credential, received by the provider with a
 * CredentialProviderBaseService.onBeginCreateCredentialRequest call.
 *
 * This request will not contain all parameters needed to store the password. Provider must
 * use the initial parameters to determine if the password can be stored, and return
 * a list of [CreateEntry], denoting the accounts/groups where the password can be stored.
 * When user selects one of the returned [CreateEntry], the corresponding [PendingIntent] set on
 * the [CreateEntry] will be fired. The [Intent] invoked through the [PendingIntent] will contain the
 * complete [CreatePasswordRequest]. This request will contain all required parameters to
 * actually store the password.
 *
 * @see BeginCreateCredentialRequest
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class BeginCreatePasswordCredentialRequest constructor(
    callingAppInfo: CallingAppInfo?,
    candidateQueryData: Bundle
) : BeginCreateCredentialRequest(
    PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    candidateQueryData,
    callingAppInfo,
) {

    /** @hide **/
    @Suppress("AcronymName")
    companion object {
        /** @hide **/
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        internal fun createFrom(data: Bundle, callingAppInfo: CallingAppInfo?):
            BeginCreatePasswordCredentialRequest {
            try {
                return BeginCreatePasswordCredentialRequest(
                    callingAppInfo, data)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }
    }
}
