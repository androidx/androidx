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
import androidx.annotation.RequiresApi
import android.service.credentials.BeginGetCredentialResponse
import androidx.annotation.NonNull
import androidx.credentials.PasswordCredential

/**
 * A request to a password provider to begin the flow of retrieving the user's saved passwords.
 *
 * Providers must use the parameters in this option to retrieve the corresponding credentials'
 * metadata, and then return them in the form of a list of [PasswordCredentialEntry]
 * set on the [BeginGetCredentialResponse].
 */
@RequiresApi(34)
class BeginGetPasswordOption internal constructor(
    candidateQueryData: Bundle
) : BeginGetCredentialOption(
    /*id=*/"",
    PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    candidateQueryData
) {

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
        internal fun createFrom(data: Bundle): BeginGetPasswordOption {
            return BeginGetPasswordOption(data)
        }

        @JvmField val CREATOR: Parcelable.Creator<BeginGetPasswordOption> = object :
            Parcelable.Creator<BeginGetPasswordOption> {
            override fun createFromParcel(p0: Parcel?): BeginGetPasswordOption {
                val baseOption = BeginGetCredentialOption.CREATOR.createFromParcel(p0)
                return createFrom(baseOption.candidateQueryData)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int): Array<BeginGetPasswordOption?> {
                return arrayOfNulls(size)
            }
        }
    }
}
