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
import androidx.credentials.CreatePublicKeyCredentialRequestPrivileged
import androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.Companion.BUNDLE_KEY_CLIENT_DATA_HASH
import androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.Companion.BUNDLE_KEY_RELYING_PARTY
import androidx.credentials.CreatePublicKeyCredentialRequestPrivileged.Companion.toCredentialDataBundle
import androidx.credentials.PublicKeyCredential
import androidx.credentials.internal.FrameworkClassParsingException

/**
 * Request to begin registering a public key credential, coming from a privileged source that
 * can call on behalf of another relying party.
 *
 * This request will not contain all parameters needed to create the public key. Provider must
 * use the initial parameters to determine if the public key can be registered, and return
 * a list of [CreateEntry], denoting the accounts/groups where the public key can be registered.
 * When user selects one of the returned [CreateEntry], the corresponding [PendingIntent] set on
 * the [CreateEntry] will be fired. The [Intent] invoked through the [PendingIntent] will contain
 * the complete [CreatePublicKeyCredentialRequestPrivileged]. This request will contain all
 * required parameters to actually register a public key.
 *
 * @property json the request json to be used for registering the public key credential
 *
 * @see BeginCreateCredentialRequest
 */
@RequiresApi(34)
class BeginCreatePublicKeyCredentialRequestPrivileged internal constructor(
    val json: String,
    val relyingParty: String,
    val clientDataHash: String,
    callingAppInfo: CallingAppInfo,
) : BeginCreateCredentialRequest(
    callingAppInfo,
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    toCredentialDataBundle(json, relyingParty, clientDataHash,
        /*preferImmediatelyAvailableCredentials=*/ false)
) {
    init {
        require(json.isNotEmpty()) { "json must not be empty" }
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
        internal fun createFrom(data: Bundle, callingAppInfo: CallingAppInfo):
            BeginCreatePublicKeyCredentialRequestPrivileged {
            try {
                val requestJson = data.getString(CreatePublicKeyCredentialRequestPrivileged
                    .BUNDLE_KEY_REQUEST_JSON)
                val rp = data.getString(BUNDLE_KEY_RELYING_PARTY)
                val clientDataHash = data.getString(BUNDLE_KEY_CLIENT_DATA_HASH)
                return BeginCreatePublicKeyCredentialRequestPrivileged(
                    requestJson!!,
                    rp!!,
                    clientDataHash!!,
                    callingAppInfo
                )
            } catch (e: Exception) {
                throw FrameworkClassParsingException()
            }
        }

        @JvmField val CREATOR:
            Parcelable.Creator<BeginCreatePublicKeyCredentialRequestPrivileged> = object :
            Parcelable.Creator<BeginCreatePublicKeyCredentialRequestPrivileged> {
            override fun createFromParcel(p0: Parcel?):
                BeginCreatePublicKeyCredentialRequestPrivileged {
                val baseRequest = BeginCreateCredentialRequest.CREATOR.createFromParcel(p0)
                return createFrom(baseRequest.data, baseRequest.callingAppInfo)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int):
                Array<BeginCreatePublicKeyCredentialRequestPrivileged?> {
                return arrayOfNulls(size)
            }
        }
    }
}