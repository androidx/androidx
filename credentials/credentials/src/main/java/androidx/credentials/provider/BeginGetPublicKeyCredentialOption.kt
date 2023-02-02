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

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.service.credentials.BeginGetCredentialOption
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * A request to begin the flow of getting passkeys from the user's public key credential provider.
 *
 * @property requestJson the privileged request in JSON format in the standard webauthn web json
 * shown [here](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson)
 *
 * @throws NullPointerException If [requestJson] is null
 * @throws IllegalArgumentException If [requestJson] is empty
 */
@RequiresApi(34)
class BeginGetPublicKeyCredentialOption internal constructor(
    candidateQueryData: Bundle,
    val requestJson: String,
) : BeginGetCredentialOption(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    candidateQueryData
) {
    init {
        require(requestJson.isNotEmpty()) { "requestJson must not be empty" }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }

    @Suppress("AcronymName")
    companion object {
        /** @hide */
        @JvmStatic
        internal fun createFrom(data: Bundle): BeginGetPublicKeyCredentialOption {
            try {
                val requestJson = data.getString(
                    GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON)
                return BeginGetPublicKeyCredentialOption(data, requestJson!!)
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }

        @JvmField val CREATOR: Parcelable.Creator<BeginGetPublicKeyCredentialOption> = object :
            Parcelable.Creator<BeginGetPublicKeyCredentialOption> {
            override fun createFromParcel(p0: Parcel?): BeginGetPublicKeyCredentialOption {
                val baseOption = BeginGetCredentialOption.CREATOR.createFromParcel(p0)
                return createFrom(baseOption.candidateQueryData)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int): Array<BeginGetPublicKeyCredentialOption?> {
                return arrayOfNulls(size)
            }
        }
    }
}
