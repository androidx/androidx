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
import android.os.Parcel
import android.os.Parcelable
import android.service.credentials.BeginCreateCredentialRequest
import android.service.credentials.CallingAppInfo
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordRequest.Companion.BUNDLE_KEY_ID
import androidx.credentials.CreatePasswordRequest.Companion.toCandidateDataBundle
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
 * @property id the id of the password to be stored
 * @throws NullPointerException If [id] is null
 * @throws IllegalArgumentException If [id] is empty
 *
 * @see BeginCreateCredentialRequest
 *
 * @hide
 */
@RequiresApi(34)
class BeginCreatePasswordCredentialRequest internal constructor(
    val id: String,
    callingAppInfo: CallingAppInfo
) : BeginCreateCredentialRequest(
    callingAppInfo,
    PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    toCandidateDataBundle()
    ) {

    init {
        require(id.isNotEmpty()) { "id must not be empty" }
    }
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }

    @Suppress("AcronymName")
    companion object CREATOR : Parcelable.Creator<BeginCreatePasswordCredentialRequest> {
        @JvmStatic
        internal fun createFrom(data: Bundle, callingAppInfo: CallingAppInfo):
            BeginCreatePasswordCredentialRequest {
            try {
                return BeginCreatePasswordCredentialRequest(
                    data.getString(BUNDLE_KEY_ID)!!,
                    callingAppInfo
                )
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }

        override fun createFromParcel(p0: Parcel?): BeginCreatePasswordCredentialRequest {
            val baseRequest = BeginCreateCredentialRequest.CREATOR.createFromParcel(p0)
            return createFrom(baseRequest.data, baseRequest.callingAppInfo)
        }

        @Suppress("ArrayReturn")
        override fun newArray(size: Int): Array<BeginCreatePasswordCredentialRequest?> {
            return arrayOfNulls(size)
        }
    }
}
