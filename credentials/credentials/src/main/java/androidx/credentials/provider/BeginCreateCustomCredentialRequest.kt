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

/**
 * Request to begin saving a custom credential, received by the provider with a
 * CredentialProviderBaseService.onBeginCreateCredentialRequest call.
 *
 * This request will not contain all parameters needed to store the credential. Provider must
 * use the initial parameters to determine if the password can be stored, and return
 * a list of [CreateEntry], denoting the accounts/groups where the password can be stored.
 * When user selects one of the returned [CreateEntry], the corresponding [PendingIntent] set on
 * the [CreateEntry] will be fired. The [Intent] invoked through the [PendingIntent] will contain the
 * complete [CreatePasswordRequest]. This request will contain all required parameters to
 * actually store the password.
 *
 * @property type the type of this custom credential type
 * @property data the request parameters for the custom credential option
 * @throws IllegalArgumentException If [type] is empty
 * @throws NullPointerException If [type] or [data] is null
 *
 * @see BeginCreateCredentialProviderRequest
 *
 * @hide
 */
@RequiresApi(34)
class BeginCreateCustomCredentialRequest internal constructor(
    override val type: String,
    val data: Bundle,
    callingAppInfo: CallingAppInfo
) : BeginCreateCredentialProviderRequest(
    type,
    callingAppInfo) {
    init {
        require(type.isNotEmpty()) { "type must not be empty" }
    }
}