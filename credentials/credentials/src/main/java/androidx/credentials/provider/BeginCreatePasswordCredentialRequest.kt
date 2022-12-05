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
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordRequest.Companion.BUNDLE_KEY_ID
import androidx.credentials.PasswordCredential

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
 * @property id the id of the password to be stored
 * @property applicationInfo the information of the calling app for which the password needs to
 * be stored
 * @throws NullPointerException If [id] is null
 * @throws IllegalArgumentException If [id] is empty
 *
 * @see BeginCreateCredentialProviderRequest
 *
 * @hide
 */
// TODO ("Add custom class similar to developer side")
@RequiresApi(34)
class BeginCreatePasswordCredentialRequest internal constructor(
    val id: String,
    applicationInfo: ApplicationInfo
) : BeginCreateCredentialProviderRequest(
    PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    applicationInfo) {

        init {
            require(id.isNotEmpty()) { "id must not be empty" }
        }

    companion object {
        @JvmStatic
        internal fun createFrom(data: Bundle, applicationInfo: ApplicationInfo):
            BeginCreatePasswordCredentialRequest {
            return BeginCreatePasswordCredentialRequest(
                data.getString(BUNDLE_KEY_ID)!!,
                // TODO("Propagate appSignature")
                applicationInfo
            )
        }
    }
}