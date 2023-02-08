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
import androidx.credentials.GetPublicKeyCredentialOptionPrivileged
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * A privileged request to get passkeys from the user's public key credential provider. The caller
 * can modify the RP. Only callers with privileged permission (e.g. user's public browser or caBLE)
 * can use this. These permissions will be introduced in an upcoming release.
 * TODO("Add specific permission info/annotation")
 *
 * @property requestJson the privileged request in JSON format in the standard webauthn web json
 * shown [here](https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptionsjson).
 * @property relyingParty the expected true RP ID which will override the one in the [requestJson],
 * where relyingParty is defined [here](https://w3c.github.io/webauthn/#rp-id) in more detail
 * @property clientDataHash a hash that is used to verify the [relyingParty] Identity
 *
 * @throws NullPointerException If any of [requestJson], [relyingParty], or [clientDataHash]
 * is null
 * @throws IllegalArgumentException If any of [requestJson], [relyingParty], or [clientDataHash]
 * is empty
 */
@RequiresApi(34)
class BeginGetPublicKeyCredentialOptionPrivileged internal constructor(
    candidateQueryData: Bundle,
    val requestJson: String,
    val relyingParty: String,
    val clientDataHash: String,
) : BeginGetCredentialOption(
    /*id=*/"",
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    candidateQueryData
) {

    init {
        require(requestJson.isNotEmpty()) { "requestJson must not be empty" }
        require(relyingParty.isNotEmpty()) { "rp must not be empty" }
        require(clientDataHash.isNotEmpty()) { "clientDataHash must not be empty" }
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
        internal fun createFrom(data: Bundle): BeginGetPublicKeyCredentialOptionPrivileged {
            try {
                val requestJson = data.getString(
                    GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_REQUEST_JSON)
                val rp = data.getString(
                    GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_RELYING_PARTY)
                val clientDataHash = data.getString(
                    GetPublicKeyCredentialOptionPrivileged.BUNDLE_KEY_CLIENT_DATA_HASH)
                return BeginGetPublicKeyCredentialOptionPrivileged(
                    data,
                    requestJson!!,
                    rp!!,
                    clientDataHash!!
                )
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }

        @JvmField val CREATOR: Parcelable.Creator<BeginGetPublicKeyCredentialOptionPrivileged> =
            object : Parcelable.Creator<BeginGetPublicKeyCredentialOptionPrivileged> {
            override fun createFromParcel(p0: Parcel?):
                BeginGetPublicKeyCredentialOptionPrivileged {
                val baseOption = BeginGetCredentialOption.CREATOR.createFromParcel(p0)
                return createFrom(baseOption.candidateQueryData)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int): Array<BeginGetPublicKeyCredentialOptionPrivileged?> {
                return arrayOfNulls(size)
            }
        }
    }
}
